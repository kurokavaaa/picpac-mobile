package by.picpac.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import by.picpac.studio.data.AppViewModel
import by.picpac.studio.data.ThemeMode
import by.picpac.studio.ui.PicPacApp
import by.picpac.studio.ui.theme.PicPacTheme

class MainActivity : ComponentActivity() {
    // ViewModel переживает поворот экрана и пересоздание окна, состояние не теряется.
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Тему выбирают в «Настройках»: как в системе, всегда светлая или всегда тёмная.
            val dark = when (vm.state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PicPacTheme(darkTheme = dark) {
                PicPacApp(vm)
            }
        }
    }

    // Приложение ушло в фон (свернули или закрыли) — сохраняем сразу, не дожидаясь отложенной записи.
    override fun onStop() {
        super.onStop()
        vm.flush()
    }
}
