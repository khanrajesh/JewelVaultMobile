package com.velox.jewelvault.utils

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Tablet Landscape Light Mode",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showBackground = true,
    showSystemUi = true,
)
@Preview(
    name = "Phone Portrait Dark Mode",
    device = "spec:width=411dp,height=891dp,dpi=420",
    showBackground = true,
    showSystemUi = true,
)
annotation class VaultPreview

