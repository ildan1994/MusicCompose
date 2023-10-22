package com.example.musictest.ui.audio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioApp() {
    val pagerState = rememberPagerState(
        pageCount = { 2 }
    )
    HorizontalPager(pagerState) { page ->
        when (page) {
            0 -> HomeScreen()
            1 -> PlayListScreen()
        }
    }
}

