package by.picpac.studio.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import by.picpac.studio.R
import by.picpac.studio.data.TextFont

/** Семейства создаём один раз при старте, а не на каждую перерисовку. */
private val families: Map<TextFont, FontFamily> = mapOf(
    TextFont.COMFORTAA to FontFamily(Font(R.font.font_comfortaa)),
    TextFont.CORMORANT to FontFamily(Font(R.font.font_cormorant)),
    TextFont.FORUM to FontFamily(Font(R.font.font_forum)),
    TextFont.MARCK_SCRIPT to FontFamily(Font(R.font.font_marck_script)),
    TextFont.AMATIC to FontFamily(Font(R.font.font_amatic_sc)),
    TextFont.MONTSERRAT to FontFamily(Font(R.font.font_montserrat)),
    TextFont.CAVEAT to FontFamily(Font(R.font.font_caveat)),
    TextFont.LOBSTER to FontFamily(Font(R.font.font_lobster)),
)

/** Шрифт надписи из списка → настоящий шрифт из res/font. */
fun TextFont.family(): FontFamily = families.getValue(this)
