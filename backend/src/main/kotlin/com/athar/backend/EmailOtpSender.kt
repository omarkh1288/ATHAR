package com.athar.backend

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Properties
import org.slf4j.LoggerFactory

internal class EmailOtpSender {
  private val logger = LoggerFactory.getLogger(EmailOtpSender::class.java)
  private val host = BackendConfig.string("ATHAR_SMTP_HOST", "athar.smtp.host").orEmpty()
  private val port = BackendConfig.int("ATHAR_SMTP_PORT", "athar.smtp.port") ?: 587
  private val username = BackendConfig.string("ATHAR_SMTP_USERNAME", "athar.smtp.username").orEmpty()
  private val password = BackendConfig.string("ATHAR_SMTP_PASSWORD", "athar.smtp.password").orEmpty()
  private val fromAddress = BackendConfig.string("ATHAR_SMTP_FROM", "athar.smtp.from").orEmpty()
  private val fromName = BackendConfig.string("ATHAR_SMTP_FROM_NAME", "athar.smtp.fromName")
    .orEmpty()
    .ifBlank { "Athar" }
  private val startTlsEnabled = BackendConfig.boolean("ATHAR_SMTP_STARTTLS", "athar.smtp.starttls") ?: true
  private val sslEnabled = BackendConfig.boolean("ATHAR_SMTP_SSL", "athar.smtp.ssl") ?: false
  private val timeoutMs = BackendConfig.int("ATHAR_SMTP_TIMEOUT_MS", "athar.smtp.timeoutMs") ?: 15000

  fun sendRegistrationOtp(email: String, fullName: String, code: String, expiresAtEpochSeconds: Long): Result<Unit> {
    if (host.isBlank() || fromAddress.isBlank()) {
      val message = "SMTP is not configured. Set ATHAR_SMTP_HOST and ATHAR_SMTP_FROM in local.properties or the environment."
      logger.error(
        "{} Registration OTP for {} <{}> was not sent.",
        message,
        fullName.ifBlank { "User" },
        email
      )
      return Result.failure(IllegalStateException(message))
    }

    return runCatching {
      val message = MimeMessage(session()).apply {
        setFrom(InternetAddress(fromAddress, fromName))
        setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
        subject = "Your Athar verification code"
        sentDate = Date()
        setContent(buildHtmlBody(fullName, code, expiresAtEpochSeconds), "text/html; charset=UTF-8")
      }
      Transport.send(message)
    }.onFailure { error ->
      logger.error("Failed to send registration OTP to {}", email, error)
    }
  }

  private fun session(): Session {
    val properties = Properties().apply {
      put("mail.smtp.host", host)
      put("mail.smtp.port", port.toString())
      put("mail.smtp.auth", (!username.isBlank() && !password.isBlank()).toString())
      put("mail.smtp.starttls.enable", startTlsEnabled.toString())
      put("mail.smtp.ssl.enable", sslEnabled.toString())
      put("mail.smtp.connectiontimeout", timeoutMs.toString())
      put("mail.smtp.timeout", timeoutMs.toString())
      put("mail.smtp.writetimeout", timeoutMs.toString())
    }

    return if (username.isBlank() || password.isBlank()) {
      Session.getInstance(properties)
    } else {
      Session.getInstance(
        properties,
        object : Authenticator() {
          override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
          }
        }
      )
    }
  }

  private fun buildHtmlBody(fullName: String, code: String, expiresAtEpochSeconds: Long): String {
    val expiresAt = DateTimeFormatter.ofPattern("HH:mm 'UTC'")
      .withZone(ZoneOffset.UTC)
      .format(Instant.ofEpochSecond(expiresAtEpochSeconds))

    val displayName = fullName.ifBlank { "there" }
    return """
      <html>
        <body style="margin:0;padding:24px;background:#f5f7fb;font-family:Arial,sans-serif;color:#1f3c5b;">
          <div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #e2e8f0;">
            <div style="background:linear-gradient(135deg,#1f3c5b,#2c4f73);padding:28px 32px;color:#ffffff;">
              <div style="font-size:28px;font-weight:700;letter-spacing:0.02em;">Athar</div>
              <div style="margin-top:8px;font-size:16px;opacity:0.88;">Confirm your email to finish creating your account.</div>
            </div>
            <div style="padding:32px;">
              <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">Hi $displayName,</p>
              <p style="margin:0 0 24px;font-size:15px;line-height:1.7;color:#475569;">
                Use the verification code below in the Athar app to complete your registration.
              </p>
              <div style="margin:0 0 24px;padding:18px 20px;border-radius:18px;background:#f8fafc;border:1px solid #d6e4f5;text-align:center;">
                <div style="font-size:13px;color:#64748b;letter-spacing:0.08em;text-transform:uppercase;">Verification Code</div>
                <div style="margin-top:10px;font-size:36px;letter-spacing:0.35em;font-weight:700;color:#1f3c5b;">$code</div>
              </div>
              <p style="margin:0;font-size:14px;line-height:1.6;color:#64748b;">
                This code expires at $expiresAt. If you did not request this, you can safely ignore this email.
              </p>
            </div>
          </div>
        </body>
      </html>
    """.trimIndent()
  }
}
