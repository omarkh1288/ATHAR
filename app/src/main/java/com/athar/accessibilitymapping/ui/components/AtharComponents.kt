package com.athar.accessibilitymapping.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  background: Color,
  leadingIcon: ImageVector? = null,
  leadingIconContentDescription: String? = null,
  contentColor: Color = Color.White,
  enabled: Boolean = true
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp),
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = background,
      contentColor = contentColor,
      disabledContainerColor = Color(0xFFCBD5E1),
      disabledContentColor = Color.White
    ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = leadingIconContentDescription,
          modifier = Modifier.size(20.dp),
          tint = contentColor
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge
      )
    }
  }
}

@Composable
fun OutlineActionButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  borderColor: Color,
  contentColor: Color
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(2.dp, borderColor),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Text(
      text = text,
      color = contentColor,
      style = MaterialTheme.typography.labelLarge
    )
  }
}

