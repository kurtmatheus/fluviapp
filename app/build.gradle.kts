import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

/**
 * Credenciais de assinatura, fora do versionamento (`keystore.properties` e `*.jks` são gitignored).
 * Ver `keystore.properties.example` para o formato e para como gerar o keystore.
 *
 * Ausente o arquivo, o `release` continua saindo **sem assinatura** — que é o comportamento de antes e o
 * que permite compilar o projeto sem ter a chave. Quem não assina, não distribui; quem só quer compilar,
 * compila.
 */
val arquivoDeAssinatura = rootProject.file("keystore.properties")
val credenciaisDeAssinatura = Properties().apply {
    if (arquivoDeAssinatura.exists()) FileInputStream(arquivoDeAssinatura).use { load(it) }
}

android {
    namespace = "dev.matheus.fluviapp"
    compileSdk = 35

    defaultConfig {
        // Identidade instalada do app, e a ÚNICA coisa aqui que a Play congela para sempre: publicado
        // uma vez, mudar o applicationId cria outro app (listing nova, base zerada). Por isso ele é
        // decidido antes do primeiro upload, e não acompanha o `namespace` acima — que é só o pacote do
        // código e pode ser refatorado quando convier.
        // `br.com.fluviapp` é reverse-DNS de domínio próprio; `dev.matheus` presumia um `matheus.dev`
        // que não existe, e `com.fluviapp` colidiria com um `fluviapp.com` de terceiros.
        applicationId = "br.com.fluviapp"
        minSdk = 26
        targetSdk = 34
        // Sobe a cada artefato distribuído: dois builds com o mesmo `versionCode` são indistinguíveis na
        // lista do tester, e nenhum crash no Crashlytics correlaciona com "qual build" (P3.3).
        versionCode = 10
        versionName = "0.0.2-alpha01"

        testInstrumentationRunner = "dev.matheus.fluviapp.CustomTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (arquivoDeAssinatura.exists()) {
                storeFile = file(credenciaisDeAssinatura.getProperty("storeFile"))
                storePassword = credenciaisDeAssinatura.getProperty("storePassword")
                keyAlias = credenciaisDeAssinatura.getProperty("keyAlias")
                keyPassword = credenciaisDeAssinatura.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Só assina se houver chave. O APK de release sem assinatura não instala em lugar nenhum —
            // era o P3.1 do roadmap, e é o que separava "o build passa" de "dá para entregar a alguém".
            if (arquivoDeAssinatura.exists()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Schema do Room exportado (app/schemas/<db>/<versao>.json): é a FONTE do DDL da MIGRATION_1_2 —
    // com o histórico de migrações colapsado (ADR-0015 §9), o `createSql` gerado aqui é o que a migração
    // executa. Sem isto, o DDL seria transcrito à mão e divergiria das entidades sem ninguém perceber.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        unitTests {
            // android.util.Log e afins viram no-op nos testes JVM (em vez de lançar "not mocked").
            isReturnDefaultValues = true

            // A suíte segue o escopo da revitalização (ADR-0020): roda o que cobre a entidade viva — hoje
            // a Empresa — e exclui o que está marcado com `@Category(ForaDoEscopo::class)`. Não é
            // desligar teste para o build ficar verde: é o mesmo recorte que o app faz em tela, aplicado
            // à suíte, para que "vermelho" volte a significar "quebrei algo que estava de pé".
            //
            // `-PsuiteCompleta` roda tudo, para medir o que falta revitalizar.
            all { teste ->
                teste.useJUnit {
                    if (!project.hasProperty("suiteCompleta")) {
                        excludeCategories("dev.matheus.fluviapp.revitalizacao.ForaDoEscopo")
                    }
                }
            }
        }
    }
}

dependencies {
    val lifecycleVersion = "2.8.7"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    // LocalLifecycleOwner p/ vincular a câmera ao ciclo de vida (ADR-0012, tela de embarque).
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")

    val navVersion = "2.8.5"
    implementation("androidx.navigation:navigation-compose:$navVersion")
    androidTestImplementation("androidx.navigation:navigation-testing:$navVersion")

    val composeBomVersion = "2024.12.01"

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    val hiltVersion = "2.53.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    kspAndroidTest("com.google.dagger:hilt-compiler:$hiltVersion")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Geração de QR (bilhete): ZXing. Leitura de QR no embarque (ADR-0012): CameraX + ML Kit.
    implementation("com.google.zxing:core:3.5.3")

    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("com.google.code.gson:gson:2.11.0")

    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-crashlytics")

    // O Credential Manager (Google Sign-In) saiu em P2.2c junto com o autocadastro: o acesso é só
    // e-mail + senha de quem a gestão pré-cadastrou (ADR-0015 §2.1).
}