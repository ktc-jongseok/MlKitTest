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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
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
// 이미지에서 라벨을 추출하는 함수
fun calculateSimilarityAndDisplayResult(
    uri1: Uri,
    uri2: Uri,
    context: Context,
    similarityResult : (Double) -> Unit
) {
    // ML Kit 이미지 라벨링을 사용하여 각 이미지에 대한 라벨링 수행
    // 예시 코드는 이미지 라벨링과 유사도 계산의 기본적인 흐름을 보여줍니다.
    // 실제 구현에서는 Firebase ML Kit 문서를 참조하여 라벨링을 수행하고 결과를 처리해야 합니다.

    val image1 = InputImage.fromFilePath(context, uri1)
    val image2 = InputImage.fromFilePath(context, uri2)
    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    // 이미지 1 라벨링
    labeler.process(image1)
        .addOnSuccessListener { labels1 ->
            // 이미지 2 라벨링
            labeler.process(image2)
                .addOnSuccessListener { labels2 ->
                    // 유사도 계산
                    val similarity = calculateSimilarity(labels1, labels2)
                    // 유사도 결과를 사용자에게 표시, 예: Toast 메시지, UI 업데이트 등
                    similarityResult(similarity)
                }
        }
        .addOnFailureListener { e ->
            // 오류 처리
            Toast.makeText(context, "Error Message: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

// 두 라벨 리스트의 유사도를 계산하는 함수
fun calculateSimilarity(labels1: List<ImageLabel>, labels2: List<ImageLabel>): Double {
    // 라벨 텍스트만 추출
    val texts1 = labels1.map { it.text }
    val texts2 = labels2.map { it.text }

    // 공통 요소 찾기
    val commonLabels = texts1.filter {
        Log.e("Lables","commonLabels : $it")
        it in texts2
    }.size


    // 유사도 계산 (공통 요소의 수를 두 리스트 길이의 합으로 나눔)
    return (commonLabels.toDouble() / (texts1.size + texts2.size - commonLabels)) * 100
}

fun resizeImage(context: Context, imageUri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)

    val aspectRatio: Float = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
    var finalWidth = reqWidth
    var finalHeight = reqHeight

    // 비율을 유지하면서 크기 조정
    if (originalBitmap.width > originalBitmap.height) {
        // 가로가 세로보다 길 경우
        finalHeight = (finalWidth / aspectRatio).toInt()
    } else {
        // 세로가 가로보다 길 경우
        finalWidth = (finalHeight * aspectRatio).toInt()
    }

    // 이미지 리사이징
    return Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true)
}

@Composable
fun ImageComparisonScreen() {
    val context = LocalContext.current
    var imageUri1 by remember { mutableStateOf<Uri?>(null) }
    var imageUri2 by remember { mutableStateOf<Uri?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }

    // 이미지 선택 결과 처리를 위한 런처
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // imageUri1 또는 imageUri2에 URI 할당하는 로직 필요
        if (imageUri1 == null) {
            imageUri1 = uri
        } else if (imageUri2 == null) {
            imageUri2 = uri
            // 이미지 두 장이 모두 선택되면 유사도 계산 함수 호출
            imageUri1?.let { uri1 ->
                imageUri2?.let { uri2 ->
                    calculateSimilarityAndDisplayResult(uri1, uri2, context) { similarity ->
                        resultText = similarity.toString()
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
                    model = ImageRequest.Builder(LocalContext.current)
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
                    model = ImageRequest.Builder(LocalContext.current)
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
        }

    }
}
