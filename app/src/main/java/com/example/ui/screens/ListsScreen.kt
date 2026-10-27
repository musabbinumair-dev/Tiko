package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Work
import com.example.data.model.CategoryColors
import com.example.data.model.CategoryIcons
import com.example.ui.components.CreateCategoryDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.TaskEntity
import com.example.ui.components.AppToastController
import com.example.ui.components.ToastType
import com.example.ui.components.AppBlack
import com.example.ui.components.EmptyState
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppCreamBackground
import com.example.ui.components.ArrowOutwardIcon
import com.example.ui.components.CardBlack
import com.example.ui.components.CardLime
import com.example.ui.components.CardPurple
import com.example.ui.components.CardYellow
import com.example.ui.components.NeoCheckbox
import com.example.ui.components.FrostedGlassPill
import com.example.ui.components.RoundIconButton
import com.example.ui.components.frostedGlass
import com.example.ui.components.screenTopPadding
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.TikoViewModel

private data class ListCardUiModel(
    val categoryName: String,
    val icon: ImageVector,
    val totalTasks: Int,
    val doneTasks: Int,
    val backgroundColor: Color,
    val contentColor: Color,
    val nextItemText: String? = null,
    val isLastTaller: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    viewModel: TikoViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    // Hardware/gesture back press navigates to Home
    BackHandler {
        viewModel.selectTab(NavigationTab.HOME)
    }

    val tasks by viewModel.allTasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Active", "Completed"
    var selectedCategoryForSheet by remember { mutableStateOf<String?>(null) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allCardModels = categories.mapIndexed { index, cat ->
        val catTasks = tasks.filter { it.category.equals(cat.name, ignoreCase = true) }
        val total = catTasks.size
        val done = catTasks.count { it.done }
        val nextPending = catTasks.firstOrNull { !it.done }?.title
        ListCardUiModel(
            categoryName = cat.name,
            icon = CategoryIcons.getIcon(cat.iconKey),
            totalTasks = total,
            doneTasks = done,
            backgroundColor = Color(cat.colorHex),
            contentColor = CategoryColors.getContentColor(cat.colorHex),
            nextItemText = if (nextPending != null) "Next: $nextPending" else null,
            isLastTaller = index == categories.size - 1
        )
    }

    // Filter cards based on filter pills selection
    val displayedCards = allCardModels.filter { card ->
        when (selectedFilter) {
            "Active" -> card.doneTasks < card.totalTasks
            "Completed" -> card.doneTasks == card.totalTasks
            else -> true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppCreamBackground)
    ) {
        // Main scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 120.dp // Clears floating bottom nav
            )
        ) {
            // 1. Top Area: Back button (RoundIconButton) at left, Search button at right
            item {
                ListsTopBar(
                    onBackClick = { viewModel.selectTab(NavigationTab.HOME) },
                    onSearchClick = { showSearchDialog = true }
                )
            }

            // 2. Title Row (16dp below): "MY LISTS" Outfit Black 44sp + "+ New list" pill
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ListsTitleRow(
                    onNewListClick = { showNewListDialog = true }
                )
            }

            // 3. Filter Pills (14dp below): "All 4", "Active", "Completed"
            item {
                Spacer(modifier = Modifier.height(14.dp))
                ListsFilterPills(
                    selectedFilter = selectedFilter,
                    totalListsCount = allCardModels.size,
                    onSelectFilter = { selectedFilter = it }
                )
            }

            // 4. Overlapping List Cards (24dp below)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                if (displayedCards.isEmpty()) {
                    EmptyState(
                        image = R.drawable.illus_no_results,
                        title = "No matches",
                        caption = "Try a different word or clear the filters.",
                        buttonText = "Clear filters",
                        onButton = { selectedFilter = "All" }
                    )
                } else {
                    OverlappingListCards(
                        cards = displayedCards,
                        onCardClick = { categoryName ->
                            selectedCategoryForSheet = categoryName
                        }
                    )
                }
            }
        }

        // Soft fade scrim behind the bar (90dp gradient, transparent to cream #EFEBDD at 80%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFEFEBDD).copy(alpha = 0.80f)
                        )
                    )
                )
        )

        // Category Detail Bottom Sheet when tapping any card
        selectedCategoryForSheet?.let { categoryName ->
            val categoryTasks = tasks.filter { it.category.equals(categoryName, ignoreCase = true) }
            CategoryTasksBottomSheet(
                categoryName = categoryName,
                tasks = categoryTasks,
                onDismiss = { selectedCategoryForSheet = null },
                onToggleTask = { task -> viewModel.toggleTaskDone(task) },
                onTaskClick = { task ->
                    selectedCategoryForSheet = null
                    onNavigate(Screen.TaskDetail(task.id))
                },
                onAddTask = {
                    selectedCategoryForSheet = null
                    viewModel.preselectedCategoryForNewTask = categoryName
                    onNavigate(Screen.AddTask)
                }
            )
        }

        // "+ New list" Overlay with icon picker
        if (showNewListDialog) {
            CreateCategoryDialog(
                titleText = "NEW LIST",
                onDismiss = { showNewListDialog = false },
                onCreateCategory = { listName, iconKey, colorHex ->
                    viewModel.addCategory(listName, iconKey, colorHex)
                    viewModel.createQuickTask("$listName starter task", listName)
                    AppToastController.show("List '$listName' created", ToastType.Success)
                    showNewListDialog = false
                    selectedCategoryForSheet = listName
                }
            )
        }

        // Search Overlay
        if (showSearchDialog) {
            ListsSearchModalOverlay(
                tasks = tasks,
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                onDismiss = { showSearchDialog = false },
                onTaskClick = { task ->
                    showSearchDialog = false
                    onNavigate(Screen.TaskDetail(task.id))
                }
            )
        }
    }
}

/**
 * Top bar matching Home:
 * Back button (RoundIconButton) at left, Search button (RoundIconButton) at right
 * Same 14dp top padding, 48dp circles, white fill, 1dp #151515 outline, 24dp icons.
 */
@Composable
private fun ListsTopBar(
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .screenTopPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back button (RoundIconButton with left chevron <)
        RoundIconButton(
            icon = Icons.Outlined.ChevronLeft,
            onClick = onBackClick,
            contentDescription = "Back",
            testTag = "lists_back_button"
        )

        // Right: Search button (RoundIconButton with search icon)
        RoundIconButton(
            icon = Icons.Outlined.Search,
            onClick = onSearchClick,
            contentDescription = "Search",
            testTag = "lists_search_button"
        )
    }
}

/**
 * Title Row (16dp below header):
 * "MY LISTS" Outfit Black 44sp, uppercase, black, letter spacing -1sp
 * At right: "+ New list" pill, white fill, 1dp black outline, height 40dp, Outfit Medium 16sp
 */
@Composable
private fun ListsTitleRow(
    onNewListClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MY LISTS",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 44.sp,
            lineHeight = 44.sp,
            letterSpacing = (-1).sp,
            color = AppBlack,
            modifier = Modifier.testTag("lists_title_my_lists")
        )

        // "+ New list" pill: frosted glassmorphic glassy pill, height 40dp, Outfit Medium 16sp
        FrostedGlassPill(
            modifier = Modifier.height(40.dp),
            onClick = onNewListClick,
            testTag = "lists_new_list_pill"
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "New list",
                    tint = AppBlack,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New list",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = AppBlack
                )
            }
        }
    }
}

/**
 * Filter pills (14dp below title):
 * "All 4" selected (black fill, white text), "Active", "Completed" (frosted glassmorphic pill)
 * height 40dp, fully rounded, Outfit Medium 15sp, 8dp gaps
 */
@Composable
private fun ListsFilterPills(
    selectedFilter: String,
    totalListsCount: Int,
    onSelectFilter: (String) -> Unit
) {
    val filters = listOf(
        "All" to "All $totalListsCount",
        "Active" to "Active",
        "Completed" to "Completed"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (key, label) ->
            val isSelected = (selectedFilter == key) || (selectedFilter == "All" && key == "All")
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(CircleShape)
                                .background(AppBlack)
                        } else {
                            Modifier.frostedGlass()
                        }
                    )
                    .clickable { onSelectFilter(key) }
                    .padding(horizontal = 18.dp)
                    .testTag("filter_pill_$key"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = if (isSelected) Color.White else AppBlack
                )
            }
        }
    }
}

/**
 * Overlapping list cards:
 * 28dp radius, full width, each card overlaps previous by about 18dp.
 * Uses layout modifier to report height minus overlapPx so following card overlaps it.
 */
@Composable
private fun OverlappingListCards(
    cards: List<ListCardUiModel>,
    onCardClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        cards.forEachIndexed { index, card ->
            val isLast = index == cards.lastIndex
            SingleOverlappingListCard(
                card = card,
                isOverlapped = !isLast,
                zIndex = index.toFloat(),
                onClick = { onCardClick(card.categoryName) }
            )
        }
    }
}

@Composable
private fun SingleOverlappingListCard(
    card: ListCardUiModel,
    isOverlapped: Boolean,
    zIndex: Float,
    onClick: () -> Unit
) {
    val cardHeight = if (card.isLastTaller) 255.dp else 192.dp
    val overlapPx = 18.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(zIndex)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val reportedHeight = if (isOverlapped) {
                    placeable.height - overlapPx.roundToPx()
                } else {
                    placeable.height
                }
                layout(placeable.width, reportedHeight) {
                    placeable.placeRelative(0, 0)
                }
            }
            .height(cardHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(card.backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("list_card_${card.categoryName.lowercase()}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Row: Category tag (left) + Arrow outward circle (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White pill tag: height 32dp, icon 18dp + Outfit Medium 14sp
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = card.categoryName,
                            tint = AppBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = card.categoryName,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = AppBlack
                        )
                    }
                }

                // 44dp white circle with black arrow_outward icon 22dp
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    ArrowOutwardIcon(
                        size = 22.dp,
                        color = AppBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title: Outfit Bold 32sp
            Text(
                text = card.categoryName,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                color = card.contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // "12 tasks" Outfit Regular 16sp (reduced opacity)
            Text(
                text = "${card.totalTasks} tasks",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = card.contentColor.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // "8 of 12 done" Outfit Medium 15sp
            Text(
                text = "${card.doneTasks} of ${card.totalTasks} done",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = card.contentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar: 8dp high fully rounded (track = 25% opacity, fill = solid)
            val progress = if (card.totalTasks > 0) {
                (card.doneTasks.toFloat() / card.totalTasks.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(card.contentColor.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(CircleShape)
                        .background(card.contentColor)
                )
            }

            // Last card only (Study): thin divider + 32dp black circle with white chevron + "Next: Read chapter 5"
            if (card.isLastTaller && card.nextItemText != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // Thin divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(card.contentColor.copy(alpha = 0.18f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 32dp black circle with white chevron
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Next Task",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = card.nextItemText,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = card.contentColor
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet displaying tasks for the selected list category.
 * Allows checking/unchecking tasks, clicking to view details, or adding a task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTasksBottomSheet(
    categoryName: String,
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onAddTask: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppCreamBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = categoryName.uppercase(),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = AppBlack
                    )
                    Text(
                        text = "${tasks.size} tasks • ${tasks.count { it.done }} completed",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = AppBlack.copy(alpha = 0.7f)
                    )
                }

                // Add Task Pill Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(AppBlack)
                        .clickable(onClick = onAddTask)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add task",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (tasks.isEmpty()) {
                EmptyState(
                    image = R.drawable.illus_empty_list,
                    title = "Nothing in here yet",
                    caption = "Add a task to this list.",
                    buttonText = "Add task",
                    onButton = onAddTask
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        CategoryTaskItem(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onClick = { onTaskClick(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, AppBlack.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppBlack
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${task.dueTime} • ${if (task.done) "Completed" else "Pending"}",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = AppBlack.copy(alpha = 0.6f)
            )
        }

        NeoCheckbox(
            checked = task.done,
            onCheckedChange = { onToggle() },
            size = 28.dp,
            modifier = Modifier.testTag("lists_checkbox_${task.id}")
        )
    }
}

/**
 * Modal overlay to add a new list/category
 */
@Composable
private fun NewListModalOverlay(
    onDismiss: () -> Unit,
    onCreateList: (String) -> Unit
) {
    var listName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppCreamBackground)
                .border(2.dp, AppBlack, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume */ }
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "NEW LIST",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = AppBlack
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    placeholder = {
                        Text(
                            text = "e.g. Travel, Finance, Projects",
                            fontFamily = OutfitFontFamily,
                            color = AppBlack.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AppBlack,
                        unfocusedBorderColor = AppBlack.copy(alpha = 0.3f),
                        focusedTextColor = AppBlack,
                        unfocusedTextColor = AppBlack
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, AppBlack, CircleShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = AppBlack
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(if (listName.isNotBlank()) AppBlack else AppBlack.copy(alpha = 0.3f))
                            .clickable(enabled = listName.isNotBlank()) {
                                onCreateList(listName.trim())
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Search Overlay to find tasks across lists
 */
@Composable
private fun ListsSearchModalOverlay(
    tasks: List<TaskEntity>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    val searchResults = if (searchQuery.isNotBlank()) {
        tasks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.notes.contains(searchQuery, ignoreCase = true)
        }
    } else emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppCreamBackground)
                .border(2.dp, AppBlack, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume */ }
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEARCH LISTS",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = AppBlack
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, AppBlack, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = AppBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "Search by task or list name...",
                            fontFamily = OutfitFontFamily,
                            color = AppBlack.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AppBlack,
                        unfocusedBorderColor = AppBlack.copy(alpha = 0.3f),
                        focusedTextColor = AppBlack,
                        unfocusedTextColor = AppBlack
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (searchResults.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { task ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .clickable { onTaskClick(task) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = task.title,
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AppBlack
                                        )
                                        Text(
                                            text = task.category,
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = AppBlack.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (task.done) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Done",
                                            tint = CardPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (searchQuery.isNotBlank()) {
                    EmptyState(
                        image = R.drawable.illus_no_results,
                        title = "No matches",
                        caption = "Try a different word or clear the filters.",
                        buttonText = "Clear filters",
                        onButton = { onQueryChange("") }
                    )
                }
            }
        }
    }
}
