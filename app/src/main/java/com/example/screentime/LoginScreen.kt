package com.example.screentime

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.screentime.network.RetrofitInstance
import com.example.screentime.ui.theme.rexliaFontFamily
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException
import android.content.Context

@Composable
fun CustomTextFieldWithDrawable(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val drawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.text_box)


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // Altura estándar de un TextField
            .drawBehind {
                drawable?.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawable?.draw(drawContext.canvas.nativeCanvas)
            }
            .padding(horizontal = 8.dp)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxSize(),
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onPasswordVisibilityChange?.invoke() }) {
                        val iconRes = if (isPasswordVisible) R.drawable.icon_eye else R.drawable.icon_eye_off
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = "Toggle Password Visibility",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Unspecified // Importante para usar el color original del ícono
                        )
                    }
                }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

fun saveUserInfo(context: Context, userId: Int, userName: String) {
    val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putInt("user_id", userId)
        putString("user_name", userName)  // 🔥 Guardar el nombre del usuario
        apply()
    }
}


@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, // Callback para manejar el éxito del inicio de sesión
    modifier: Modifier = Modifier
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val btnred: Drawable? = ContextCompat.getDrawable(context, R.drawable.btn_red)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF121212), // Gris oscuro
                        Color(0xFF1A237E)  // Azul oscuro
                    )
                )
            )
    ) {
        // Column principal que contendrá dos Columnas internas
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Columna para logo y nombre de la aplicación (arriba de la pantalla)
            Column(
                verticalArrangement = Arrangement.Top, // Ubica este contenido en la parte superior
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .padding(bottom = 50.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                // Logo de la aplicación
                Image(
                    painter = painterResource(id = R.drawable.ic_icon), // Cambia esto con tu logo
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp) // Espaciado debajo del logo
                )

                // Nombre de la aplicación con tamaño de texto más grande
                Text(
                    text = "Screen Time Analyzer",
                    fontFamily = rexliaFontFamily,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp, // Tamaño más grande para el nombre de la aplicación
                    style = TextStyle(
                        color = Color.White, // Color del texto principal
                        shadow = Shadow(
                            color = Color.Black,      // Color de la sombra
                            offset = Offset(0f, 10f), // Desplazamiento de la sombra
                            blurRadius = 30f          // Radio de desenfoque
                        )
                    ),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                )
            }

            // Columna para los campos de inicio de sesión (en el centro)
            Column(
                verticalArrangement = Arrangement.Top, // Centra este contenido
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Texto "Iniciar sesión bGames"
                Text(
                    text = "Iniciar sesión bGames",
                    fontFamily = rexliaFontFamily,
                    color = Color.White,
                    fontSize = 20.sp, // Tamaño moderado para el texto
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        color = Color.White, // Color del texto principal
                        shadow = Shadow(
                            color = Color.Black,      // Color de la sombra
                            offset = Offset(0f, 10f), // Desplazamiento de la sombra
                            blurRadius = 30f          // Radio de desenfoque
                        )
                    ),
                    modifier = Modifier
                        .padding(bottom = 24.dp) // Espaciado antes de los campos de entrada
                )

                // Campo de texto para el correo electrónico
                CustomTextFieldWithDrawable(
                    value = email.value,
                    onValueChange = {
                        email.value = it
                        Log.d("LoginScreen", "Email updated: ${email.value}") // Log para verificar si el email cambia
                    },
                    label = "Correo Electrónico",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de texto para la contraseña
                CustomTextFieldWithDrawable(
                    value = password.value,
                    onValueChange = {
                        password.value = it
                        Log.d("LoginScreen", "Password updated: ${password.value}") // Log para verificar si la contraseña cambia
                    },
                    label = "Contraseña",
                    modifier = Modifier.fillMaxWidth(),
                    isPassword = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordVisibilityChange = {
                        isPasswordVisible = !isPasswordVisible
                        Log.d("LoginScreen", "Password visibility changed: $isPasswordVisible") // Log para verificar si la visibilidad cambia
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón de inicio de sesión
                Button(
                    onClick = {
                        Log.d("LoginScreen", "Login button clicked") // Log cuando se presiona el botón de login
                        if (email.value.isNotEmpty() && password.value.isNotEmpty()) {
                            isLoading = true // Indicar que el login está en proceso
                            Log.d("LoginScreen", "Login started for email: ${email.value}") // Log para verificar que el login ha comenzado
                            login(
                                context = context,
                                email = email.value,
                                password = password.value,
                                onSuccess = {
                                    isLoading = false
                                    Log.d("LoginScreen", "Login successful for email: $email") // Log para éxito en login
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(it)
                                },
                                onError = {
                                    isLoading = false
                                    Log.d("LoginScreen", "Login started for email: ${email.value}") // Log para fallo en login
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            btnred?.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                            btnred?.draw(drawContext.canvas.nativeCanvas)
                        }
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontFamily = rexliaFontFamily,
                        style = TextStyle(
                            color = Color.White, // Color del texto principal
                            shadow = Shadow(
                                color = Color.Black,      // Color de la sombra
                                offset = Offset(0f, 10f), // Desplazamiento de la sombra
                                blurRadius = 20f          // Radio de desenfoque
                            )
                        )
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

data class Player(
    val name: String,
    val password: String,
    val email: String,
    val age: Int,
    val external_type: String,
    val external_id: Int,
    val id_players: Int
)

fun login(
    context: Context,
    email: String,
    password: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    Log.d("LoginScreen", "Login attempt started for email: $email and password: $password")
    CoroutineScope(Dispatchers.IO).launch {
        try {
            withTimeout(5000L) { // Timeout de 5 segundos
                // Obtener jugador por email
                val playerResponse = RetrofitInstance.userApi().getPlayerByEmail(email)
                Log.d("LoginScreen", "Response from getPlayerByEmail: ${playerResponse.isSuccessful}")

                if (playerResponse.isSuccessful && playerResponse.body() != null) {
                    // Convertir el ResponseBody en un objeto Player usando Gson
                    val playerData = playerResponse.body()?.string()
                    Log.d("LoginScreen", "Player response body: $playerData")
                    val player = Gson().fromJson(playerData, Player::class.java)
                    Log.d("LoginScreen", "Player data: $player")

                    val userName = player.name
                    val userId = player.id_players
                    val userIdString = player.id_players.toString()
                    saveUserInfo(context, userId, userName)
                    Log.d("LoginScreen", "Username: $userName")

                    // Ahora que tenemos el nombre de usuario, hacemos el login
                    val loginResponse = RetrofitInstance.userApi().verifyCredentials(userName, password)
                    if (loginResponse.isSuccessful) {
                        val body = loginResponse.body()?.string() // Convertir ResponseBody en texto
                        Log.d("LoginScreen", "Login response: $body")

                        // Procesar el cuerpo de la respuesta
                        if (body == userIdString) { // Login exitoso
                            Log.d("LoginScreen", "Login successful: $body")
                            withContext(Dispatchers.Main) { onSuccess(email) }
                        } else { // Credenciales incorrectas
                            Log.d("LoginScreen", "Login failed: Invalid credentials")
                            withContext(Dispatchers.Main) { onError("Credenciales incorrectas") }
                        }
                    } else {
                        when (loginResponse.code()) {
                            400 -> {
                                Log.d("LoginScreen", "Login failed: Invalid credentials (400)")
                                withContext(Dispatchers.Main) { onError("Usuario y/o contraseña incorrectos") }
                            }
                            else -> {
                                Log.d("LoginScreen", "Server error with code: ${loginResponse.code()}")
                                withContext(Dispatchers.Main) { onError("Error del servidor: ${loginResponse.code()}") }
                            }
                        }
                    }
                } else {
                    Log.d("LoginScreen", "Player not found for email: $email")
                    withContext(Dispatchers.Main) { onError("Jugador no encontrado") }
                }
            }
        } catch (e: TimeoutCancellationException) {
            // Error por timeout
            Log.e("LoginScreen", "Timeout error: ${e.message}")
            withContext(Dispatchers.Main) { onError("La operación tardó demasiado. Puede haber problemas con el servidor.") }
        } catch (e: HttpException) {
            // Errores de HTTP específicos
            Log.e("LoginScreen", "HTTP error: ${e.message}")
            withContext(Dispatchers.Main) { onError("No se pudo conectar al servidor. Código HTTP: ${e.code()}") }
        } catch (e: IOException) {
            // Error de red o servidor no disponible
            Log.e("LoginScreen", "Network error: ${e.message}")
            withContext(Dispatchers.Main) { onError("No se pudo conectar al servidor. Verifica tu conexión a Internet.") }
        } catch (e: Exception) {
            // Otros errores no previstos
            Log.e("LoginScreen", "Unknown error: ${e.message}")
            withContext(Dispatchers.Main) { onError("Error desconocido: ${e.message}") }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onLoginSuccess = {}, // Proporcionar una función vacía
        modifier = Modifier
    )
}


