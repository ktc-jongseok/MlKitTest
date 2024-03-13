package com.example.mlkittest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mlkittest.ui.theme.MlKitTestTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MlKitTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ImageComparisonScreen()
                }
            }
        }
    }
}
// 画像からラベルを抽出するFunction
fun calculateSimilarityAndDisplayResult(
    uri1: Uri,
    uri2: Uri,
    context: Context,
    similarityResult : (Pair<Double, List<String>>) -> Unit
) {
    // ML Kit イメージラベリングを使用した各イメージのラベリングの実行
    // 画像ラベリングと類似度の計算

    val image1 = InputImage.fromFilePath(context, uri1)
    val image2 = InputImage.fromFilePath(context, uri2)
    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    // Image 1 ラベリング
    labeler.process(image1)
        .addOnSuccessListener { labels1 ->
            // Image 2 ラベリング
            labeler.process(image2)
                .addOnSuccessListener { labels2 ->
                    // 類似度の計算
                    val similarity = calculateSimilarity(labels1, labels2)
                    // 類似度の結果を表示
                    similarityResult(similarity)
                }
        }
        .addOnFailureListener { e ->
            // エラーの処理
            Toast.makeText(context, "Error Message: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

// 2つのラベルリストの類似度を計算するFunction
fun calculateSimilarity(
    labels1: List<ImageLabel>,
    labels2: List<ImageLabel>
): Pair<Double, List<String>> {
    // ラベルテキストのみ抽出
    val texts1 = labels1.map { it.text }
    val texts2 = labels2.map { it.text }

    // 共通要素を探す
    val commonLabels = texts1.filter {
        Log.e("Lables","commonLabels : $it")
        it in texts2
    }

    // 類似度の計算（共通要素の数を2つのリスト長の合計で除算）
    return Pair((commonLabels.size.toDouble() / (texts1.size + texts2.size - commonLabels.size)) * 100, commonLabels)
}

fun resizeImage(context: Context, imageUri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)

    val aspectRatio: Float = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
    var finalWidth = reqWidth
    var finalHeight = reqHeight

    // 比率を維持しながらサイズ変更
    if (originalBitmap.width > originalBitmap.height) {
        // 横が縦よりも長い場合
        finalHeight = (finalWidth / aspectRatio).toInt()
    } else {
        // 縦が横より長い場合
        finalWidth = (finalHeight * aspectRatio).toInt()
    }

    // イメージリサイジング
    return Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true)
}

@Composable
fun ImageComparisonScreen() {
    val context = LocalContext.current
    var imageUri1 by remember { mutableStateOf<Uri?>(null) }
    var imageUri2 by remember { mutableStateOf<Uri?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var commonTags by remember { mutableStateOf<String?>(null) }

    // 画像選択結果処理のためのランチャー
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (imageUri1 == null) {
            imageUri1 = uri
        } else if (imageUri2 == null) {
            imageUri2 = uri
            // 両方の画像を選択した場合、類似度計算関数を呼び出す
            imageUri1?.let { uri1 ->
                imageUri2?.let { uri2 ->
                    calculateSimilarityAndDisplayResult(uri1, uri2, context) { similarity ->
                        resultText = similarity.first.toString()
                        commonTags = similarity.second.joinToString(separator = "-")
                    }
                }
            }
        }
    }

    LazyColumn(modifier = Modifier
        .padding(16.dp)
    ) {
        item {
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Text("Select First Image")
            }
        }

        item(imageUri1) {
            imageUri1?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resizeImage(context, uri, 800, 600))
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.baseline_downloading_24),
                    contentDescription = "Selected Image1",
                    error = painterResource(id = R.drawable.baseline_error_24),
                )
            }
        }

        item {
            Button(onClick = {
                launcher.launch("image/*")
            }) {
                Text("Select Second Image")
            }
        }

        item(imageUri2) {
            imageUri2?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resizeImage(context, uri, 800, 600))
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.baseline_downloading_24),
                    contentDescription = "Selected Image2",
                    error = painterResource(id = R.drawable.baseline_error_24),
                )
            }
        }

        item {
            Button(onClick = {
                imageUri1 = null
                imageUri2 = null
                resultText = null
                commonTags = null
            }) {
                Text("Clear Images")
            }
        }

        item {
            resultText?.let {
                Text(
                    text = "Two Pictures similarity is \n$it %",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            commonTags?.let {
                Text(
                    text = "Tags : $it",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
            }
        }
    }
}
