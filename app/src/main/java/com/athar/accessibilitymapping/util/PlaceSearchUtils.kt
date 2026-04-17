package com.athar.accessibilitymapping.util

import com.athar.accessibilitymapping.data.Location
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

data class SearchScore(
  val tier: Int,
  val editDistance: Int = 0,
  val startIndex: Int = Int.MAX_VALUE,
  val tokenGapPenalty: Int = Int.MAX_VALUE,
  val lengthDelta: Int = Int.MAX_VALUE
) : Comparable<SearchScore> {
  override fun compareTo(other: SearchScore): Int {
    return compareValuesBy(
      this,
      other,
      SearchScore::tier,
      SearchScore::editDistance,
      SearchScore::startIndex,
      SearchScore::tokenGapPenalty,
      SearchScore::lengthDelta
    )
  }
}

fun normalizePlaceSearchText(value: String): String {
  if (value.isBlank()) return ""
  return Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace("[أإآٱ]".toRegex(), "ا")
    .replace("ى", "ي")
    .replace("ؤ", "و")
    .replace("ئ", "ي")
    .replace("ة", "ه")
    .replace("ـ", "")
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
}

fun buildLocationSearchCandidates(location: Location): List<String> {
  val featureKeywords = buildList {
    add("accessible accessibility disabled disability wheelchair")
    if (location.features.ramp) add("ramp wheelchair ramp")
    if (location.features.elevator) add("elevator lift")
    if (location.features.accessibleToilet) add("accessible toilet restroom bathroom wc")
    if (location.features.accessibleParking) add("accessible parking parking")
    if (location.features.wideEntrance) add("wide entrance entrance")
    if (location.features.brailleSignage) add("braille signage")
  }

  return buildList {
    add(location.name)
    add(location.category)
    add("${location.name} ${location.category}")
    addAll(featureKeywords)
    addAll(location.recentReports.take(3))
  }
}

fun scorePlaceSearch(query: String, candidates: List<String>): SearchScore? {
  val normalizedQuery = normalizePlaceSearchText(query)
  if (normalizedQuery.isBlank()) return null
  return candidates
    .mapNotNull { candidate -> scoreCandidate(normalizedQuery, candidate) }
    .minOrNull()
}

private fun scoreCandidate(normalizedQuery: String, rawCandidate: String): SearchScore? {
  val candidate = normalizePlaceSearchText(rawCandidate)
  if (candidate.isBlank()) return null

  val queryTokens = normalizedQuery.split(' ').filter { it.isNotBlank() }
  val candidateWords = candidate.split(' ').filter { it.isNotBlank() }
  val lengthDelta = abs(candidate.length - normalizedQuery.length)

  if (candidate == normalizedQuery) {
    return SearchScore(tier = 0, startIndex = 0, tokenGapPenalty = 0, lengthDelta = lengthDelta)
  }
  if (candidate.startsWith(normalizedQuery)) {
    return SearchScore(tier = 1, startIndex = 0, tokenGapPenalty = 0, lengthDelta = lengthDelta)
  }

  val wordPrefixIndex = candidateWords.indexOfFirst { it.startsWith(normalizedQuery) }
  if (wordPrefixIndex >= 0) {
    return SearchScore(tier = 2, startIndex = wordPrefixIndex, tokenGapPenalty = 0, lengthDelta = lengthDelta)
  }

  matchQueryTokens(queryTokens, candidateWords, prefixOnly = true)?.let { coverage ->
    return SearchScore(
      tier = 3,
      startIndex = coverage.firstIndex,
      tokenGapPenalty = coverage.gapPenalty,
      lengthDelta = lengthDelta
    )
  }

  val containsIndex = candidate.indexOf(normalizedQuery)
  if (containsIndex >= 0) {
    return SearchScore(tier = 4, startIndex = containsIndex, tokenGapPenalty = 0, lengthDelta = lengthDelta)
  }

  matchQueryTokens(queryTokens, candidateWords, prefixOnly = false)?.let { coverage ->
    return SearchScore(
      tier = 5,
      startIndex = coverage.firstIndex,
      tokenGapPenalty = coverage.gapPenalty,
      lengthDelta = lengthDelta
    )
  }

  val acronym = candidateWords.joinToString(separator = "") { it.take(1) }
  if (acronym.isNotBlank() && acronym.startsWith(normalizedQuery)) {
    return SearchScore(tier = 6, startIndex = 0, tokenGapPenalty = 0, lengthDelta = abs(acronym.length - normalizedQuery.length))
  }

  val fullWordDistance = candidateWords.minOfOrNull { word ->
    boundedLevenshtein(normalizedQuery, word, maxTypoDistance(normalizedQuery, word))
  }
  if (fullWordDistance != null && fullWordDistance <= maxTypoDistance(normalizedQuery, normalizedQuery)) {
    return SearchScore(tier = 7, editDistance = fullWordDistance, startIndex = 0, tokenGapPenalty = 0, lengthDelta = lengthDelta)
  }

  matchQueryTokensFuzzy(queryTokens, candidateWords)?.let { coverage ->
    return SearchScore(
      tier = 8,
      editDistance = coverage.totalEditDistance,
      startIndex = coverage.firstIndex,
      tokenGapPenalty = coverage.gapPenalty,
      lengthDelta = lengthDelta
    )
  }

  subsequenceGap(normalizedQuery, candidate)?.let { gap ->
    return SearchScore(tier = 9, editDistance = gap, startIndex = 0, tokenGapPenalty = gap, lengthDelta = lengthDelta)
  }

  return null
}

private data class TokenCoverage(
  val firstIndex: Int,
  val gapPenalty: Int,
  val totalEditDistance: Int = 0
)

private fun matchQueryTokens(
  queryTokens: List<String>,
  candidateWords: List<String>,
  prefixOnly: Boolean
): TokenCoverage? {
  if (queryTokens.isEmpty() || candidateWords.isEmpty()) return null
  val usedIndices = mutableSetOf<Int>()
  val matchedIndices = mutableListOf<Int>()

  for (token in queryTokens) {
    val index = candidateWords.indices.firstOrNull { idx ->
      idx !in usedIndices && if (prefixOnly) {
        candidateWords[idx].startsWith(token)
      } else {
        candidateWords[idx].contains(token)
      }
    } ?: return null
    usedIndices += index
    matchedIndices += index
  }

  return buildTokenCoverage(matchedIndices)
}

private fun matchQueryTokensFuzzy(
  queryTokens: List<String>,
  candidateWords: List<String>
): TokenCoverage? {
  if (queryTokens.isEmpty() || candidateWords.isEmpty()) return null
  val usedIndices = mutableSetOf<Int>()
  val matchedIndices = mutableListOf<Int>()
  var totalEditDistance = 0

  for (token in queryTokens) {
    val bestMatch = candidateWords.indices
      .filter { it !in usedIndices }
      .map { index ->
        index to boundedLevenshtein(token, candidateWords[index], maxTypoDistance(token, candidateWords[index]))
      }
      .filter { (_, distance) -> distance <= maxTypoDistance(token, token) }
      .minByOrNull { (_, distance) -> distance }
      ?: return null

    usedIndices += bestMatch.first
    matchedIndices += bestMatch.first
    totalEditDistance += bestMatch.second
  }

  val coverage = buildTokenCoverage(matchedIndices) ?: return null
  return coverage.copy(totalEditDistance = totalEditDistance)
}

private fun buildTokenCoverage(matchedIndices: List<Int>): TokenCoverage? {
  if (matchedIndices.isEmpty()) return null
  val sortedIndices = matchedIndices.sorted()
  val gapPenalty = sortedIndices
    .zipWithNext()
    .sumOf { (left, right) -> maxOf(0, right - left - 1) }
  return TokenCoverage(
    firstIndex = sortedIndices.first(),
    gapPenalty = gapPenalty
  )
}

private fun maxTypoDistance(left: String, right: String): Int {
  return when (min(left.length, right.length)) {
    in 0..4 -> 1
    in 5..8 -> 2
    else -> 3
  }
}

private fun boundedLevenshtein(left: String, right: String, maxDistance: Int): Int {
  if (abs(left.length - right.length) > maxDistance) return maxDistance + 1
  if (left == right) return 0

  var previous = IntArray(right.length + 1) { it }
  var current = IntArray(right.length + 1)

  for (i in left.indices) {
    current[0] = i + 1
    var rowMin = current[0]
    for (j in right.indices) {
      val substitutionCost = if (left[i] == right[j]) 0 else 1
      current[j + 1] = min(
        min(previous[j + 1] + 1, current[j] + 1),
        previous[j] + substitutionCost
      )
      rowMin = min(rowMin, current[j + 1])
    }
    if (rowMin > maxDistance) return maxDistance + 1
    val swap = previous
    previous = current
    current = swap
  }

  return previous[right.length]
}

private fun subsequenceGap(query: String, candidate: String): Int? {
  if (query.isBlank() || candidate.isBlank()) return null
  var queryIndex = 0
  var gap = 0

  for (candidateIndex in candidate.indices) {
    if (queryIndex >= query.length) break
    if (candidate[candidateIndex] == query[queryIndex]) {
      queryIndex += 1
    } else if (queryIndex > 0) {
      gap += 1
    }
  }

  return if (queryIndex == query.length) gap else null
}
