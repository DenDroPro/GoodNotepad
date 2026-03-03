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
    // Pastel colors
    PASTEL_RED(Color(0xFFFFCDD2)),
    PASTEL_BLUE(Color(0xFFBBDEFB)),
    PASTEL_LIGHT_BLUE(Color(0xFFB2EBF2)),
    PASTEL_GREEN(Color(0xFFC8E6C9)),
    PASTEL_PURPLE(Color(0xFFE1BEE7)),
    PASTEL_YELLOW(Color(0xFFFFF9C4)),
    PASTEL_ORANGE(Color(0xFFFFE0B2)),
    PASTEL_PINK(Color(0xFFF8BBD0)),
    // Vibrant colors
    VIBRANT_RED(Color(0xFFEF5350)),
    VIBRANT_BLUE(Color(0xFF42A5F5)),
    VIBRANT_LIGHT_BLUE(Color(0xFF29B6F6)),
    VIBRANT_GREEN(Color(0xFF66BB6A)),
    VIBRANT_PURPLE(Color(0xFFAB47BC)),
    VIBRANT_YELLOW(Color(0xFFFFEE58)),
    VIBRANT_ORANGE(Color(0xFFFFA726)),
    VIBRANT_PINK(Color(0xFFEC407A))
}

enum class FolderColor(val color: Color) {
    // Default brown for backward compatibility
    BROWN(Color(0xFFBCAAA4)),
    // Pastel
    PASTEL_RED(Color(0xFFFFCDD2)),
    PASTEL_BLUE(Color(0xFFBBDEFB)),
    PASTEL_LIGHT_BLUE(Color(0xFFB2EBF2)),
    PASTEL_GREEN(Color(0xFFC8E6C9)),
    PASTEL_PURPLE(Color(0xFFE1BEE7)),
    PASTEL_YELLOW(Color(0xFFFFF9C4)),
    PASTEL_ORANGE(Color(0xFFFFE0B2)),
    PASTEL_PINK(Color(0xFFF8BBD0)),
    // Vibrant
    VIBRANT_RED(Color(0xFFEF5350)),
    VIBRANT_BLUE(Color(0xFF42A5F5)),
    VIBRANT_LIGHT_BLUE(Color(0xFF29B6F6)),
    VIBRANT_GREEN(Color(0xFF66BB6A)),
    VIBRANT_PURPLE(Color(0xFFAB47BC)),
    VIBRANT_YELLOW(Color(0xFFFFEE58)),
    VIBRANT_ORANGE(Color(0xFFFFA726)),
    VIBRANT_PINK(Color(0xFFEC407A))
}

enum class FolderIcon {
    FOLDER, WORK, SCHOOL, FAVORITE, MUSIC, PHOTO, VIDEO, TRAVEL,
    FOOD, SPORT, HEALTH, FINANCE, SHOPPING, PETS, ART, CODE,
    BOOK, GAME, HOME, CAR,
    CROSS, FLOWER, MONEY, RECEIPT, STAR, BABY, CHURCH, NATURE, SCIENCE, PHONE
}

enum class HighlightColor(val color: Color) {
    NONE(Color.Transparent),
    YELLOW(Color(0xFFFFFF00)),
    GREEN(Color(0xFF00FF00)),
    BLUE(Color(0xFF87CEEB)),
    PINK(Color(0xFFFFB6C1)),
    ORANGE(Color(0xFFFFA500))
}

enum class FontColor(val color: Color, val label: String) {
    BLACK(Color(0xFF333333), "\u0427\u0451\u0440\u043d\u044b\u0439"),
    DARK_GRAY(Color(0xFF555555), "\u0422\u0451\u043c\u043d\u043e-\u0441\u0435\u0440\u044b\u0439"),
    GRAY(Color(0xFF888888), "\u0421\u0435\u0440\u044b\u0439"),
    BROWN(Color(0xFF5D4037), "\u041a\u043e\u0440\u0438\u0447\u043d\u0435\u0432\u044b\u0439"),
    RED(Color(0xFFD32F2F), "\u041a\u0440\u0430\u0441\u043d\u044b\u0439"),
    BLUE(Color(0xFF1976D2), "\u0421\u0438\u043d\u0438\u0439"),
    GREEN(Color(0xFF388E3C), "\u0417\u0435\u043b\u0451\u043d\u044b\u0439"),
    PURPLE(Color(0xFF7B1FA2), "\u0424\u0438\u043e\u043b\u0435\u0442\u043e\u0432\u044b\u0439"),
    ORANGE(Color(0xFFE65100), "\u041e\u0440\u0430\u043d\u0436\u0435\u0432\u044b\u0439"),
    TEAL(Color(0xFF00695C), "\u0411\u0438\u0440\u044e\u0437\u043e\u0432\u044b\u0439"),
    WHITE(Color(0xFFFFFFFF), "\u0411\u0435\u043b\u044b\u0439")
}

enum class ViewMode {
    LIST,
    GRID_2,
    GRID_3,
    GRID_4,
    GRID_5
}

enum class SortMode(val label: String) {
    CREATED_DESC("\u041f\u043e \u0434\u0430\u0442\u0435 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u044f (\u043d\u043e\u0432\u044b\u0435)"),
    CREATED_ASC("\u041f\u043e \u0434\u0430\u0442\u0435 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u044f (\u0441\u0442\u0430\u0440\u044b\u0435)"),
    UPDATED_DESC("\u041f\u043e \u0434\u0430\u0442\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f (\u043d\u043e\u0432\u044b\u0435)"),
    UPDATED_ASC("\u041f\u043e \u0434\u0430\u0442\u0435 \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f (\u0441\u0442\u0430\u0440\u044b\u0435)"),
    TITLE_ASC("\u041f\u043e \u0430\u043b\u0444\u0430\u0432\u0438\u0442\u0443 \u0410-\u042f"),
    TITLE_DESC("\u041f\u043e \u0430\u043b\u0444\u0430\u0432\u0438\u0442\u0443 \u042f-\u0410")
}
