package com.example.tradutor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tradutor.ui.theme.TradutorTheme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.speech.RecognizerIntent
import android.content.Intent
import java.util.Locale

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TradutorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

fun traducao(texto: String, Retorno: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Usando 'use' para garantir que o cliente fecha e libera conexões de rede com segurança
            val resultadoLimpo = HttpClient(CIO).use { client ->
                val response: HttpResponse = client.post("https://translate.libregalaxy.org/translate") {
                    contentType(ContentType.Application.Json)
                    // Adicionamos headers para simular um cliente legítimo
                    header(HttpHeaders.UserAgent, "AndroidTranslatorApp")
                    setBody(
                        """
                        {
                            "q": "$texto",
                            "source": "pt",
                            "target": "en",
                            "format": "text"
                        }
                    """.trimIndent()
                    )
                }

                val resultado = response.bodyAsText()

                if (resultado.contains("translatedText")) {
                    resultado.substringAfter("\"translatedText\":\"").substringBefore("\"")
                } else {
                    resultado
                }
            }

            withContext(Dispatchers.Main) {
                Retorno(resultadoLimpo)
            }
        } catch (e: Exception) {
            e.printStackTrace() // Imprime a pilha de erros completa no Logcat do Android Studio
            val mensagemErro = if (!e.message.isNullOrBlank()) e.message else e.javaClass.simpleName

            withContext(Dispatchers.Main) {
                Retorno("Erro: $mensagemErro")
            }
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    var texto = remember { mutableStateOf("") }
    val speechLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {

            // 2. Pega a lista de textos que o Android reconheceu da sua fala
            val textosFalados = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            // 3. Se a lista não estiver vazia, pega o primeiro texto e atualiza a tela
            if (!textosFalados.isNullOrEmpty()) {
                texto.value = textosFalados[0]
                traducao(texto = texto.value) {Retorno ->
                    texto.value = Retorno
                }
            }
        }
    }



    fun processo(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Pega o idioma do celular
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Pode falar, estou ouvindo...")
        }

        speechLauncher.launch(intent) // Captura do audio


    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { // Tela
        Surface(modifier = Modifier.padding(top = 60.dp)) {
            Text(
                text = texto.value,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) { // Botão embaixo
            Button(
                onClick = { // Botão clicado
                    processo()
                },
                contentPadding = PaddingValues(
                    horizontal = 120.dp,
                    vertical = 12.dp
                )
            ) {
                Text(
                    text = "\uD83C\uDFA4",
                    fontSize = 50.sp,
                    modifier = Modifier.padding(all = 20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TradutorTheme {
        Greeting()
    }
}