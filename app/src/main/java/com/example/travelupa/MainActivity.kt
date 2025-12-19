package com.example.travelupa

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextAlign
import com.example.travelupa.ui.theme.TravelupaTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import android.net.Uri
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import com.example.travelupa.AppDatabase
import com.example.travelupa.ImageDao
import com.example.travelupa.ImageEntity
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            TravelupaTheme {
                Surface (modifier = Modifier.fillMaxSize(),
                    color = Color.White){
                    AppNavigation()
                }
            }
        }
    }
}
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object RekomendasiTempat : Screen("rekomendasi_tempat")
    object Gallery : Screen("gallery")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.RekomendasiTempat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.RekomendasiTempat.route) {
            RekomendasiTempatScreen(
                navController = navController,
                onBackToLogin = {
                    navController.navigateUp()
                }
            )
        }
        composable(Screen.Gallery.route) {
            val context = LocalContext.current
            val db = remember {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "travelupa-database"
                ).build()
            }
            GalleryScreen(
                imageDao = db.imageDao(),
                onImageSelected = { uri ->
                    // uri from gallery items is actually a local file path string; store as-is.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedImageLocalPath", uri.toString())
                    navController.navigateUp()
                },
                onBack = { navController.navigateUp() }
            )
        }
    }
}

@Composable
fun GreetingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Selamat Datang di Travelupa!",
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Solusi buat kamu yang lupa kemana-mana",
            style = MaterialTheme.typography.h6
        )
    }

        Button(
            onClick = {/*TODO*/},
            modifier = Modifier
                .width(360.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(text = "Mulai")
        }
    }
}

    data class TempatWisata(
        val nama: String="",
        val deskripsi: String="",
        val gambarUriString: String? = null,
        val gambarResId: Int? = null
    )

@Composable
fun RekomendasiTempatScreen(
    navController: androidx.navigation.NavHostController,
    onBackToLogin: (()->Unit)?=null
) {
    val firestore = FirebaseFirestore.getInstance()
    var daftarTempatWisata by remember {
        mutableStateOf(
            listOf(
                TempatWisata(
                    "Tumpak Sewu",
                    "Air terjun tercantik di Jawa Timur.",
                    gambarResId = R.drawable.tumpak_sewu_best_waterfall_indonesia_java_foot
                ),
                TempatWisata(
                    "Gunung Bromo",
                    "Matahari terbitnya bagus banget.",
                    gambarResId = R.drawable.bromo
                )
            )
        )
    }

    LaunchedEffect(Unit) {
        fetchRekomendasiTempatWisata(
            firestore = firestore,
            onSuccess = { fetched ->
                if (fetched.isNotEmpty()) {
                    daftarTempatWisata = fetched
                }
            },
            onFailure = { e ->
                Log.w("RekomendasiTempatScreen", "Failed to fetch tempat_wisata from Firestore", e)
                // keep default list
            }
        )
    }

    // Listen for gallery result (savedStateHandle)
    val selectedImageLocalPath by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("selectedImageLocalPath", "")
        ?.collectAsState(initial = "")
        ?: remember { mutableStateOf("") }

    var showTambahDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedImageLocalPath) {
        if (selectedImageLocalPath.isNotBlank()) {
            showTambahDialog = true
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selectedImageLocalPath")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTambahDialog = true },
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Tempat Wisata")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn {
                items(daftarTempatWisata) { tempat ->
                    TempatItemEditable(
                        tempat = tempat,
                        onDelete = {
                            daftarTempatWisata = daftarTempatWisata.filter { it != tempat }
                        }
                    )
                }
            }

            // Dialog Tambah Tempat Wisata
            if (showTambahDialog) {
                TambahTempatWisataDialog(
                    firestore = firestore,
                    context = LocalContext.current,
                    initialGambarLocalPath = selectedImageLocalPath.ifBlank { null },
                    onOpenGallery = {
                        showTambahDialog = false
                        navController.navigate(Screen.Gallery.route)
                    },
                    onDismiss = { showTambahDialog = false },
                    onTambah = { nama, deskripsi, gambarLocalPath ->
                        val path = gambarLocalPath ?: ""
                        val nuevoTempat = TempatWisata(nama, deskripsi, path)
                        daftarTempatWisata = daftarTempatWisata + nuevoTempat
                        showTambahDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun TempatItemEditable(
    tempat: TempatWisata,
    onDelete: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colors.surface),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(
                painter = tempat.gambarUriString?.let { uriString ->
                    rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(uriString))
                            .build()
                    )
                } ?: tempat.gambarResId?.let {
                    painterResource(id = it)
                } ?: painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = tempat.nama,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Text(
                        text = tempat.nama,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp, top = 12.dp)
                    )
                    Text(
                        text = tempat.deskripsi,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(250.dp, 0.dp),
                ) {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        firestore.collection("tempat_wisata").document(tempat.nama)
                            .delete()
                            .addOnSuccessListener {
                                onDelete()
                            }
                            .addOnFailureListener { e: Exception ->
                                Log.w("TempatItemEditable", "Error deleting document", e)
                            }
                    }) {
                        Text("Delete")
                    }
                }
            }}}}

@Composable
fun TambahTempatWisataDialog(
    firestore: FirebaseFirestore,
    context: Context,
    initialGambarLocalPath: String? = null,
    onOpenGallery: () -> Unit,
    onDismiss: () -> Unit,
    onTambah: (String, String, String?) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var gambarUri by remember { mutableStateOf<Uri?>(null) }
    var gambarLocalPath by remember { mutableStateOf<String?>(initialGambarLocalPath) }
    var isUploading by remember { mutableStateOf(false) }

    val gambarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        gambarUri = uri
        gambarLocalPath = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tempat Wisata Baru") },
        text = {
            Column {
                TextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Tempat") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    gambarUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(model = gambarUri),
                            contentDescription = "Gambar yang dipilih",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !gambarLocalPath.isNullOrBlank() -> {
                        Image(
                            painter = rememberAsyncImagePainter(model = gambarLocalPath),
                            contentDescription = "Gambar yang dipilih",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { gambarLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("Pilih Gambar")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenGallery,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Text("Ambil Foto (Gallery)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && deskripsi.isNotBlank() && (gambarUri != null || !gambarLocalPath.isNullOrBlank())) {
                        isUploading = true
                        val tempatWisata = TempatWisata(nama, deskripsi)

                        when {
                            gambarUri != null -> {
                                uploadImageToFirestore(
                                    firestore,
                                    context,
                                    gambarUri!!,
                                    tempatWisata,
                                    onSuccess = { uploadedTempat ->
                                        isUploading = false
                                        onTambah(nama, deskripsi, uploadedTempat.gambarUriString)
                                        onDismiss()
                                    },
                                    onFailure = { _ ->
                                        isUploading = false
                                    }
                                )
                            }
                            !gambarLocalPath.isNullOrBlank() -> {
                                uploadImageToFirestoreWithLocalPath(
                                    firestore,
                                    context,
                                    gambarLocalPath!!,
                                    tempatWisata,
                                    onSuccess = { uploadedTempat ->
                                        isUploading = false
                                        onTambah(nama, deskripsi, uploadedTempat.gambarUriString)
                                        onDismiss()
                                    },
                                    onFailure = { _ ->
                                        isUploading = false
                                    }
                                )
                            }
                        }
                    }
                },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Tambah")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface),
                enabled = !isUploading
            ) {
                Text("Batal")
            }
        }
    )
}

fun uploadImageToFirestore(
    firestore: FirebaseFirestore,
    context: Context,
    imageUri: Uri,
    tempatWisata: TempatWisata,
    onSuccess: (TempatWisata) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "travelupa-database"
    ).build()

    val imageDao = db.imageDao()

    try {
        val localPath = saveImageLocally(context, imageUri)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                imageDao.insert(ImageEntity(localPath = localPath))

                val updatedTempatWisata = tempatWisata.copy(gambarUriString = localPath)
                firestore.collection("tempat_wisata")
                    .add(updatedTempatWisata)
                    .addOnSuccessListener { onSuccess(updatedTempatWisata) }
                    .addOnFailureListener { e -> onFailure(e) }
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    } catch (e: Exception) {
        onFailure(e)
    }
}

// New helper for local file path (already saved by GalleryScreen/Room)
fun uploadImageToFirestoreWithLocalPath(
    firestore: FirebaseFirestore,
    context: Context,
    localPath: String,
    tempatWisata: TempatWisata,
    onSuccess: (TempatWisata) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "travelupa-database"
    ).build()

    val imageDao = db.imageDao()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // keep same behavior: store to Room as well
            imageDao.insert(ImageEntity(localPath = localPath))

            val updatedTempatWisata = tempatWisata.copy(gambarUriString = localPath)
            firestore.collection("tempat_wisata")
                .add(updatedTempatWisata)
                .addOnSuccessListener { onSuccess(updatedTempatWisata) }
                .addOnFailureListener { e -> onFailure(e) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}

fun saveImageLocally(context: Context, uri: Uri): String {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        Log.d("ImageSave", "Image saved successfully to: ${file.absolutePath}")
        return file.absolutePath
    } catch (e: Exception) {
        Log.e("ImageSave", "Error saving image", e)
        throw e
    }
}



@Composable
fun GambarPicker(
    gambarUri: Uri?,
    onPilihGambar: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tampilkan gambar jika sudah dipilih
        gambarUri?.let { uri ->
            Image(painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .build()
            ),
                contentDescription = "Gambar Tempat Wisata",
                modifier = Modifier
                    .size(200.dp)
                    .clickable { onPilihGambar() },
                contentScale = ContentScale.Crop)
        } ?: run {
            OutlinedButton(
                onClick = onPilihGambar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Pilih Gambar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Gambar")
            }
        }
    }
}
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter email and password"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                coroutineScope.launch {
                    try {
                        // Firebase Authentication
                        val authResult = withContext(Dispatchers.IO) {
                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                .await()
                        }
                        isLoading = false
                        onLoginSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Login failed: ${e.localizedMessage}"
                    }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
        ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White
            )
        } else {
            Text("Login")
        }
    }

        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    imageDao: ImageDao,
    onImageSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val images by imageDao.getAllImages().collectAsState(initial = emptyList())
    var showAddImageDialog by remember { mutableStateOf(false) }
    var selectedImageEntity by remember { mutableStateOf<ImageEntity?>(null) }
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf<ImageEntity?>(null) }

    LaunchedEffect(images) {
        Log.d("GalleryScreen", "Total images: ${images.size}")
        images.forEachIndexed { index, image ->
            Log.d("GalleryScreen", "Image $index path: ${image.localPath}")
            val file = File(image.localPath)
            Log.d("GalleryScreen", "File exists: ${file.exists()}, is readable: ${file.canRead()}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddImageDialog = true },
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Image")
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(paddingValues)
        ) {
            items(count = images.size) { index ->
                val image = images[index]
                Image(
                    painter = rememberAsyncImagePainter(
                        model = image.localPath
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                        .clickable {
                            selectedImageEntity = image
                            onImageSelected(Uri.parse(image.localPath))
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (showAddImageDialog) {
            AddImageDialog(
                onDismiss = { showAddImageDialog = false },
                onImageAdded = { uri ->
                    try {
                        val localPath = saveImageLocally(context, uri)
                        val newImage = ImageEntity(localPath = localPath)
                        CoroutineScope(Dispatchers.IO).launch {
                            imageDao.insert(newImage)
                        }
                        showAddImageDialog = false
                    } catch (e: Exception) {
                        Log.e("ImageSave", "Failed to save image", e)
                    }
                }
            )
        }

        selectedImageEntity?.let { imageEntity ->
            ImageDetailDialog(
                imageEntity = imageEntity,
                onDismiss = { selectedImageEntity = null },
                onDelete = { imageToDelete ->
                    showDeleteConfirmation = imageToDelete
                }
            )
        }

        showDeleteConfirmation?.let { imageToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Image") },
                text = { Text("Are you sure you want to delete this image?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                imageDao.delete(imageToDelete)
                                val file = File(imageToDelete.localPath)
                                if (file.exists()) {
                                    file.delete()
                                }
                            }
                            showDeleteConfirmation = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmation = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AddImageDialog(
    onDismiss: () -> Unit,
    onImageAdded: (Uri) -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToUri(context, it)
            imageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Image") },
        text = {
            Column {
                imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select from File")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Take Photo")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {imageUri?.let { uri ->
                    onImageAdded(uri) } }
            ){
                Text("Add")
            } },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel") }
        }
    )
}
@Composable
fun ImageDetailDialog(
    imageEntity: ImageEntity,
    onDismiss: () -> Unit,
    onDelete: (ImageEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Image(
                painter = rememberAsyncImagePainter(model = imageEntity.localPath),
                contentDescription = "Detailed Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        },
        confirmButton = {
            Row {
                Button(onClick = { onDelete(imageEntity) }) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

fun saveBitmapToUri(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "${UUID.randomUUID()}.jpg")
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    outputStream.close()
    return Uri.fromFile(file)
}

fun fetchRekomendasiTempatWisata(
    firestore: FirebaseFirestore,
    onSuccess: (List<TempatWisata>) -> Unit,
    onFailure: (Exception) -> Unit
) {
    firestore.collection("tempat_wisata")
        .get()
        .addOnSuccessListener { querySnapshot ->
            try {
                val result = querySnapshot.documents.mapNotNull { doc ->
                    val nama = doc.getString("nama") ?: doc.getString("Nama") ?: ""
                    val deskripsi = doc.getString("deskripsi") ?: doc.getString("Deskripsi") ?: ""
                    val gambarUriString = doc.getString("gambarUriString")
                        ?: doc.getString("gambar")
                        ?: doc.getString("image")

                    if (nama.isBlank() || deskripsi.isBlank()) {
                        null
                    } else {
                        TempatWisata(
                            nama = nama,
                            deskripsi = deskripsi,
                            gambarUriString = gambarUriString,
                            gambarResId = null
                        )
                    }
                }
                onSuccess(result)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}
