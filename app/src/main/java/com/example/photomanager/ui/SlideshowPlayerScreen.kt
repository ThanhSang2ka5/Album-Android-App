package com.example.photomanager.ui

import android.media.MediaPlayer
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.photomanager.SlideEffect
import com.example.photomanager.SlideshowConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@Composable
fun SlideshowPlayerScreen(config: SlideshowConfig, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val photos = config.photos
    val scope = rememberCoroutineScope()

    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState { photos.size }
    var playing by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showShareSheet by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    // Ẩn thanh hệ thống, giữ màn hình luôn sáng trong lúc trình chiếu
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Quản lý MediaPlayer cho nhạc nền
    val player = remember(config.musicUri) {
        config.musicUri?.let { uri ->
            runCatching {
                MediaPlayer().apply {
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                }
            }.getOrNull()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player?.runCatching { stop() }
            player?.release()
        }
    }

    LaunchedEffect(player, playing, showShareSheet) {
        player?.runCatching {
            if (playing && !showShareSheet) start() else pause()
        }
    }

    LaunchedEffect(player, muted) {
        player?.runCatching {
            val vol = if (muted) 0f else 1f
            setVolume(vol, vol)
        }
    }

    // Tự động chuyển slide theo thời gian đã chọn
    LaunchedEffect(pagerState.currentPage, playing, showShareSheet) {
        progress = 0f
        if (!playing || showShareSheet) return@LaunchedEffect
        val step = 40L
        var elapsed = 0L
        while (elapsed < config.intervalMs) {
            delay(step)
            elapsed += step
            progress = (elapsed.toFloat() / config.intervalMs).coerceIn(0f, 1f)
        }
        val next = pagerState.currentPage + 1
        if (next < photos.size) {
            pagerState.animateScrollToPage(next)
        } else if (config.loopSlideshow) {
            pagerState.animateScrollToPage(0)
        } else {
            playing = false
        }
    }

    // Tự ẩn thanh điều khiển sau 3.5 giây
    LaunchedEffect(controlsVisible, playing) {
        if (controlsVisible && playing) {
            delay(3500)
            controlsVisible = false
        }
    }

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // Lớp nền mờ nghệ thuật (Ambient Backdrop) phía sau
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(currentPhoto.path))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
                .graphicsLayer { alpha = 0.35f }
        )

        // Trình chiếu ảnh chính kèm hiệu ứng chuyển cảnh
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = false
        ) { page ->
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val amount = abs(offset).coerceIn(0f, 1f)
            val direction = if (page % 2 == 0) 1f else -1f

            AsyncImage(
                model = ImageRequest.Builder(context).data(File(photos[page].path)).build(),
                contentDescription = photos[page].title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        when (config.effect) {
                            SlideEffect.FADE -> {
                                translationX = offset * size.width
                                alpha = 1f - amount
                            }
                            SlideEffect.SLIDE -> {
                                alpha = 1f - (amount * 0.25f)
                            }
                            SlideEffect.KEN_BURNS -> {
                                translationX = offset * size.width + (progress * 26f * direction)
                                translationY = progress * 14f * -direction
                                val s = 1.02f + (0.14f * progress) * (1f - amount)
                                scaleX = s
                                scaleY = s
                                alpha = 1f - amount
                            }
                            SlideEffect.DEPTH_ZOOM -> {
                                translationX = offset * size.width
                                val s = 0.82f + 0.18f * (1f - amount)
                                scaleX = s
                                scaleY = s
                                rotationY = offset * -18f
                                alpha = 1f - amount
                            }
                        }
                    }
            )
        }

        // Thanh Story Progress + Header ở phía trên
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.75f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Dải thanh tiến trình dạng Story (tối đa hiển thị 20 vạch, nếu nhiều hơn thì hiện 1 thanh tổng)
            if (photos.size <= 20) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    photos.forEachIndexed { idx, _ ->
                        val segProgress = when {
                            idx < pagerState.currentPage -> 1f
                            idx == pagerState.currentPage -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.28f)
                        )
                    }
                }
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f)
                )
            }

            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Thoát")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Ảnh ${pagerState.currentPage + 1} / ${photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (config.musicTitle != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = config.musicTitle,
                                    color = Color.White.copy(alpha = 0.82f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (player != null) {
                        IconButton(
                            onClick = { muted = !muted },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Bật/tắt tiếng nhạc nền"
                            )
                        }
                    }
                    IconButton(
                        onClick = { showShareSheet = true },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Chia sẻ ảnh đang chiếu")
                    }
                }
            }
        }

        // Khu vực chú thích ảnh + Bộ điều khiển phát ở cạnh dưới
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            if (config.showCaptions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentPhoto.locationTag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "📍 ${currentPhoto.locationTag}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = dayLabel(currentPhoto.dateTaken),
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (currentPhoto.title.isNotBlank()) {
                        Text(
                            text = currentPhoto.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (currentPhoto.caption.isNotBlank()) {
                        Text(
                            text = currentPhoto.caption,
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tint = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    IconButton(onClick = {
                        scope.launch {
                            val prev = (pagerState.currentPage - 1 + photos.size) % photos.size
                            pagerState.animateScrollToPage(prev)
                        }
                    }, colors = tint) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Ảnh trước", modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(
                        onClick = { playing = !playing },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        colors = tint
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Tạm dừng" else "Phát",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage + 1) % photos.size)
                        }
                    }, colors = tint) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Ảnh sau", modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        SocialShareBottomSheet(
            photos = listOf(currentPhoto),
            onDismiss = { showShareSheet = false }
        )
    }
}
