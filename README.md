# S펜 핫키 (S Pen Hotkey)

갤럭시 탭의 S펜 사이드 버튼을 **한 번 클릭 / 두 번 클릭 / 길게 누르기**로 구분해서 원하는 동작(마우스 우클릭, 복사/붙여넣기, 뒤로가기, 특정 앱 실행 등)을 실행하는 Android 앱입니다. 기본 동작을 전체 앱에 적용하고, 원하는 앱에서는 다르게 동작하도록 앱별로 따로 설정할 수 있습니다.

## 요구 사항

- **Android 14 (API 34) 이상.** S펜 사이드 버튼 클릭이 `KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY`라는 표준 키 이벤트로 시스템에 전달되는 것은 Android 14부터입니다. 그 이전 버전에서는 루팅 없이 버튼을 안정적으로 가로챌 방법이 없어서, 이 프로젝트는 `minSdk 34`로 설정되어 있습니다.
- S펜을 지원하는 갤럭시 탭 (또는 다른 Android 14+ 스타일러스 기기).
- 빌드: Android Studio (Koala 이상 권장) 또는 명령줄의 `./gradlew`.

## 빌드 방법

1. Android Studio에서 이 폴더를 엽니다 (또는 `./gradlew assembleDebug`를 실행).
2. 최초 실행 시 Android Studio가 필요한 SDK 플랫폼(34)과 빌드 도구를 자동으로 내려받습니다.
3. `app-debug.apk`를 갤럭시 탭에 설치합니다 (`adb install app/build/outputs/apk/debug/app-debug.apk` 또는 Android Studio에서 기기로 바로 실행).

> **참고:** 이 코드는 샌드박스 환경(Android SDK/에뮬레이터 없음, `dl.google.com`/Google Maven 접근 차단)에서 작성되었기 때문에 실제로 빌드하거나 실기기에서 테스트하지 못했습니다. API 사용법은 AOSP 소스와 공식 문서를 근거로 신중하게 작성했지만, Android Studio로 처음 빌드할 때 Gradle/AGP/Compose 버전 경고나 사소한 수정이 필요할 수 있습니다.

## 설치 후 설정

1. 앱을 실행하고 "접근성 서비스 켜기"를 눌러 **설정 > 접근성 > 설치된 앱 > S펜 핫키**에서 서비스를 켭니다.
2. 삼성 설정의 **설정 > S펜 (또는 고급 기능) > S펜 버튼**을 꺼두는 것을 권장합니다. 켜져 있으면 삼성의 자체 에어 커맨드 등과 충돌할 수 있습니다.
3. "기본 동작 설정"에서 한 번 클릭 / 두 번 클릭 / 길게 누르기에 실행할 동작을 고릅니다. 기본값은 **길게 누르기 = 마우스 우클릭**입니다.
4. "앱별로 다르게 설정"에서 특정 앱(예: 포토샵, 클립스튜디오)을 선택해 그 앱에서만 다른 동작을 지정할 수 있습니다.

## 지원하는 동작

- **마우스 우클릭 (컨텍스트 메뉴)** — 포커스된 화면 요소에 `ACTION_CONTEXT_CLICK`(우클릭에 해당하는 접근성 액션)을 실행하고, 지원하지 않으면 길게 누르기 액션 → 롱프레스 제스처 순으로 대체합니다.
- 복사 / 잘라내기 / 붙여넣기 / 전체 선택
- 뒤로가기 / 홈 / 최근 앱 / 알림 패널 / 빠른 설정 / 스크린샷 / 화면 잠금 / 화면 분할
- 특정 앱 실행

## 구현하지 않은 것 (알려진 한계)

- **임의의 커스텀 키 조합 (예: Ctrl+Z, Alt+Tab)**: 루팅되지 않은 Android에서 서드파티 앱이 시스템 전역으로 임의의 키 입력을 주입하려면 [Shizuku](https://shizuku.rikka.app/)(ADB 수준 권한을 앱에 부여하는 도구) 같은 별도 권한 계층이 필요합니다. 현재 코드에는 이 경로가 구현되어 있지 않습니다. 확장하려면 `service/ActionExecutor.kt`에 Shizuku 기반 실행기를 추가하고 `data/ActionSpec.kt`에 `KeyCombo` 변형을 추가하는 방식을 권장합니다.
- Samsung S Pen Remote SDK(BLE 기반, S펜을 화면에서 떼고 있어도 버튼 인식)는 사용하지 않았습니다. 이 SDK는 일부 상위 기종(BLE S펜 탑재 모델)에서만 동작하고 Samsung 개발자 포털에서 별도로 내려받아야 해서, 대신 더 널리 호환되는 표준 Android `KeyEvent` 방식을 사용했습니다. 즉, 이 앱은 **S펜이 화면에 닿아 있거나 화면 위에서 호버링 중일 때** 버튼 입력을 인식합니다.

## 아키텍처

- `service/SPenAccessibilityService.kt` — `AccessibilityService.onKeyEvent()`로 S펜 버튼 이벤트를 받아 클릭 횟수/길게 누르기를 판정하고, 현재 포그라운드 앱에 맞는 매핑을 실행합니다.
- `service/ActionExecutor.kt` — `ActionSpec`을 실제 접근성 액션/제스처/글로벌 액션으로 변환합니다.
- `data/` — 매핑 데이터 모델과 Jetpack DataStore 기반 저장소(`MappingRepository`).
- `ui/` — Jetpack Compose 화면 (홈, 기본 동작 설정, 앱 목록, 앱별 설정).
