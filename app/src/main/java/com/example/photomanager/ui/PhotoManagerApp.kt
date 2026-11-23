package com.example.photomanager.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.photomanager.PhotoViewModel
import com.example.photomanager.data.Photo

private enum class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    TIMELINE("timeline", "Dòng thời gian", Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary),
    ALBUMS("albums", "Album", Icons.Outlined.PhotoAlbum, Icons.Filled.PhotoAlbum),
    SLIDESHOW("slideshow", "Trình chiếu", Icons.Outlined.Slideshow, Icons.Filled.Slideshow)
}

private const val ROUTE_ALBUM = "album/{albumId}"
private const val ROUTE_VIEWER = "viewer/{photoId}/{albumId}"
private const val ROUTE_PLAYER = "player"

@Composable
fun PhotoManagerApp(vm: PhotoViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val photos by vm.photos.collectAsState()
    val albums by vm.albums.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val message = vm.snackbarMessage
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            vm.clearSnackbar()
        }
    }

    val showBottomBar = Tab.entries.any { it.route == route }

    fun goToTab(tab: Tab) {
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun filterPhotosForAlbumRoute(albumId: Long, all: List<Photo>): List<Photo> {
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 86_400_000L
        return when (albumId) {
            -1L -> all
            SMART_ALBUM_FAVORITES -> all.filter { it.isFavorite }
            SMART_ALBUM_RECENT -> all.filter { it.dateTaken >= sevenDaysAgo }
            SMART_ALBUM_UNASSIGNED -> all.filter { it.albumId == null }
            else -> all.filter { it.albumId == albumId }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goToTab(tab) },
                            icon = {
                                val badgeCount = when (tab) {
                                    Tab.SLIDESHOW -> vm.slideshowPreselect.size
                                    else -> 0
                                }
                                if (badgeCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                                        Icon(
                                            if (selected) tab.selectedIcon else tab.icon,
                                            contentDescription = tab.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.label
                                    )
                                }
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.TIMELINE.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.TIMELINE.route) {
                PhotoBrowserScreen(
                    vm = vm,
                    title = "Album",
                    subtitle = "Dòng thời gian • Quản lý ảnh thông minh",
                    photos = photos,
                    albumId = null,
                    showDashboard = true,
                    onOpenViewer = { nav.navigate("viewer/${it.id}/-1") },
                    onSlideshowWith = { ids ->
                        vm.slideshowPreselect = ids
                        goToTab(Tab.SLIDESHOW)
                    }
                )
            }

            composable(Tab.ALBUMS.route) {
                AlbumsScreen(
                    vm = vm,
                    onOpenAlbum = { nav.navigate("album/$it") },
                    onStartAlbumSlideshow = { ids ->
                        vm.slideshowPreselect = ids
                        goToTab(Tab.SLIDESHOW)
                    }
                )
            }

            composable(
                ROUTE_ALBUM,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType })
            ) { entry ->
                val albumId = entry.arguments?.getLong("albumId") ?: -1L
                val (albumTitle, albumSub) = when (albumId) {
                    SMART_ALBUM_FAVORITES -> "Ảnh Yêu thích ❤️" to "Bộ sưu tập tự động các bức ảnh bạn đã thả tim"
                    SMART_ALBUM_RECENT -> "7 ngày gần đây" to "Những bức ảnh mới chụp và lưu trong tuần qua"
                    SMART_ALBUM_UNASSIGNED -> "Chưa phân loại" to "Các bức ảnh chưa được xếp vào album nào"
                    else -> {
                        val found = albums.firstOrNull { it.id == albumId }
                        (found?.name ?: "Album") to (found?.description?.ifBlank { null })
                    }
                }
                val filteredList = filterPhotosForAlbumRoute(albumId, photos)
                PhotoBrowserScreen(
                    vm = vm,
                    title = albumTitle,
                    subtitle = albumSub,
                    photos = filteredList,
                    albumId = if (albumId > 0) albumId else null,
                    showDashboard = false,
                    onOpenViewer = { nav.navigate("viewer/${it.id}/$albumId") },
                    onSlideshowWith = { ids ->
                        vm.slideshowPreselect = ids
                        goToTab(Tab.SLIDESHOW)
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Tab.SLIDESHOW.route) {
                SlideshowSetupScreen(vm = vm, onStart = { nav.navigate(ROUTE_PLAYER) })
            }

            composable(
                ROUTE_VIEWER,
                arguments = listOf(
                    navArgument("photoId") { type = NavType.LongType },
                    navArgument("albumId") { type = NavType.LongType }
                )
            ) { entry ->
                val photoId = entry.arguments?.getLong("photoId") ?: -1L
                val albumId = entry.arguments?.getLong("albumId") ?: -1L
                val list = filterPhotosForAlbumRoute(albumId, photos)
                ViewerScreen(
                    vm = vm,
                    photos = list,
                    startPhotoId = photoId,
                    onBack = { nav.popBackStack() },
                    onDelete = { photo -> vm.deletePhotos(listOf(photo.id)) }
                )
            }

            composable(ROUTE_PLAYER) {
                val config = vm.slideshow
                if (config == null) {
                    nav.popBackStack()
                } else {
                    SlideshowPlayerScreen(config = config, onClose = { nav.popBackStack() })
                }
            }
        }
    }
}
