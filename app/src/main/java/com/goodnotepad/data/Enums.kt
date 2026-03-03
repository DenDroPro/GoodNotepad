package com.goodnotepad.data

import androidx.compose.ui.graphics.Color

enum class NoteTheme(val color: Color) {
    BEIGE(Color(0xFFFFF8E7)),
    YELLOW_LIGHT(Color(0xFFFFFDE7)),
    YELLOW(Color(0xFFFFF9C4)),
    LIGHT_BLUE(Color(0xFFE3F2FD)),
    LIGHT_GREEN(Color(0xFFE8F5E9)),
    LIGHT_PINK(Color(0xFFFCE4EC)),
    LIGHT_PURPLE(Color(0xFFF3E5F5)),
    WHITE(Color(0xFFFFFFFF))
}

enum class PageStyle {
    LINED,
    GRID,
    DOTTED,
    BLANK
}

enum class TextAlign {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

enum class HeaderColor(val color: Color) {
    NONE(Color.Transparent),
    PASTEL_YELLOW(Color(0xFFFFF9C4)),
    PASTEL_BLUE(Color(0xFFB3E5FC)),
    PASTEL_PURPLE(Color(0xFFE1BEE7)),
    PASTEL_GREEN(Color(0xFFC8E6C9)),
    PASTEL_RED(Color(0xFFFFCDD2))
}

enum class FolderColor(val color: Color) {
    BROWN(Color(0xFF8D6E63)),
    BLUE(Color(0xFF42A5F5)),
    GREEN(Color(0xFF66BB6A)),
    RED(Color(0xFFEF5350)),
    PURPLE(Color(0xFFAB47BC)),
    ORANGE(Color(0xFFFFA726)),
    PINK(Color(0xFFF48FB1)),
    LIGHT_BLUE(Color(0xFF81D4FA)),
    LIGHT_GREEN(Color(0xFFA5D6A7)),
    YELLOW(Color(0xFFFFE082))
}

enum class FolderIcon {
    FOLDER,
    WORK,
    SCHOOL,
    FAVORITE,
    MUSIC,
    PHOTO,
    VIDEO,
    TRAVEL,
    FOOD,
    SPORT,
    HEALTH,
    FINANCE,
    SHOPPING,
    PETS,
    ART,
    CODE,
    BOOK,
    GAME,
    HOME,
    CAR,
    CROSS,
    FLOWER,
    MONEY,
    RECEIPT,
    STAR,
    BABY,
    CHURCH,
    NATURE,
    SCIENCE,
    PHONE
}

enum class HighlightColor(val color: Color) {
    NONE(Color.Transparent),
    YELLOW(Color(0xFFFFFF00)),
    GREEN(Color(0xFF00FF00)),
    BLUE(Color(0xFF87CEEB)),
    PINK(Color(0xFFFFB6C1)),
    ORANGE(Color(0xFFFFA500))
}

enum class ViewMode {
    LIST,
    GRID_2,
    GRID_3,
    GRID_4,
    GRID_5
}

enum class SortMode(val label: String) {
    CREATED_DESC("По дате создания (новые)"),
    CREATED_ASC("По дате создания (старые)"),
    UPDATED_DESC("По дате изменения (новые)"),
    UPDATED_ASC("По дате изменения (старые)"),
    TITLE_ASC("По алфавиту А-Я"),
    TITLE_DESC("По алфавиту Я-А")
}
