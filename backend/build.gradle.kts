plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot") version "3.3.4"
}

group = "com.woong.vibebass"
version = "0.0.1-SNAPSHOT"

// Java 및 Kotlin 컴파일러 버전을 17로 일관성 있게 정렬
kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot 공식 BOM 플랫폼을 활용한 버전 관리로 플러그인 호환성 오류 원천 해결
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Kotlin 객체 JSON 직렬화 지원
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient용
    
    runtimeOnly("org.postgresql:postgresql")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1") // 코틀린 스타일 모킹 지원
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") // Gradle 9.x 테스트 기동 호환용 추가
}

// KotlinCompile 태스크는 jvm 호환 17로 기본 매핑되므로 별도 옵션 제거

val copyWebDist = tasks.register<Copy>("copyWebDist") {
    // webApp의 배포판 빌드가 완료된 이후 수행하도록 순서 정의
    dependsOn(":webApp:wasmJsBrowserDistribution")
    
    // webApp 빌드 dist 폴더로부터 파일 긁어오기
    from(project(":webApp").layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    // 정적 자원으로 패키징하기 위해 backend 리소스 static 폴더로 복사
    into(layout.projectDirectory.dir("src/main/resources/static"))
}

// Gradle 9.x 빌드 검증 호환: processResources 태스크가 copyWebDist에 의존하도록 명시
tasks.processResources {
    dependsOn(copyWebDist)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
