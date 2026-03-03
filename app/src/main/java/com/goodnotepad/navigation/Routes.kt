package com.goodnotepad.navigation

object Routes {
    const val HOME = "home"
    const val NOTES = "notes/{folderId}"
    const val ALL_NOTES = "all_notes"
    const val FAVORITES = "favorites"
    const val TRASH = "trash"
    const val EDITOR = "editor/{noteId}"
    const val SETTINGS = "settings"

    fun notes(folderId: Long) = "notes/$folderId"
    fun editor(noteId: Long) = "editor/$noteId"
}
