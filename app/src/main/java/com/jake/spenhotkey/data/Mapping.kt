package com.jake.spenhotkey.data

/** The three S Pen side-button gestures and the action bound to each. */
data class Mapping(
    val singleClick: ActionSpec = ActionSpec.None,
    val doubleClick: ActionSpec = ActionSpec.None,
    val longPress: ActionSpec = ActionSpec.RightClick
)
