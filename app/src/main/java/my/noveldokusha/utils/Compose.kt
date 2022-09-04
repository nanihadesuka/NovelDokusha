package my.noveldokusha.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun <T> rememberMutableStateOf(value: T) = remember { mutableStateOf(value) }