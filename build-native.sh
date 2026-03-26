#!/bin/bash
# =============================================================================
#  build-native.sh — Gera instaladores nativos para macOS e Linux
#  Para Windows use: build-windows.ps1
# =============================================================================
set -e

APP_NAME="FinancasPessoais"
APP_VERSION="1.0.0"
MAIN_CLASS="com.finance.Launcher"
MAIN_JAR="personal-finance.jar"
VENDOR="Personal Finance"
DESCRIPTION="Controle de finanças pessoais"

JAVA_HOME_21="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
JPACKAGE="$JAVA_HOME_21/bin/jpackage"

# Detecta OS
OS="$(uname -s)"

# Flags JVM comuns
JVM_OPTS=(
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
    "--add-opens=java.base/java.util=ALL-UNNAMED"
    "--add-opens=java.base/java.io=ALL-UNNAMED"
    "-Dfile.encoding=UTF-8"
    "-Djava.awt.headless=false"
)

# Argumentos comuns do jpackage
COMMON_ARGS=(
    --input          "target/"
    --main-jar       "$MAIN_JAR"
    --main-class     "$MAIN_CLASS"
    --app-version    "$APP_VERSION"
    --vendor         "$VENDOR"
    --description    "$DESCRIPTION"
    --dest           "dist/"
    --java-options   "${JVM_OPTS[0]}"
    --java-options   "${JVM_OPTS[1]}"
    --java-options   "${JVM_OPTS[2]}"
    --java-options   "${JVM_OPTS[3]}"
    --java-options   "${JVM_OPTS[4]}"
)

echo "🧹 Limpando builds anteriores..."
mvn clean package -DskipTests -q
mkdir -p dist

# =============================================================================
#  macOS — .dmg
# =============================================================================
if [ "$OS" = "Darwin" ]; then
    echo "🍎 Gerando instalador macOS (.dmg)..."
    "$JPACKAGE" \
        "${COMMON_ARGS[@]}" \
        --name           "$APP_NAME" \
        --type           dmg \
        --mac-package-name "$APP_NAME" \
        --mac-package-identifier "com.finance.personal"

    echo "✅ macOS: dist/${APP_NAME}-${APP_VERSION}.dmg"

# =============================================================================
#  Linux — .deb e .rpm
# =============================================================================
elif [ "$OS" = "Linux" ]; then
    # .deb (Debian, Ubuntu, Mint, Pop!_OS, etc.)
    if command -v dpkg &>/dev/null; then
        echo "🐧 Gerando instalador Linux (.deb)..."
        jpackage \
            "${COMMON_ARGS[@]}" \
            --name        "${APP_NAME}" \
            --type        deb \
            --linux-package-name "financas-pessoais" \
            --linux-app-category "Finance" \
            --linux-shortcut
        echo "✅ Linux deb: dist/${APP_NAME}-${APP_VERSION}.deb"
    fi

    # .rpm (Fedora, RHEL, openSUSE, etc.)
    if command -v rpm &>/dev/null; then
        echo "🐧 Gerando instalador Linux (.rpm)..."
        jpackage \
            "${COMMON_ARGS[@]}" \
            --name        "${APP_NAME}" \
            --type        rpm \
            --linux-package-name "financas-pessoais" \
            --linux-app-category "Finance" \
            --linux-shortcut
        echo "✅ Linux rpm: dist/${APP_NAME}-${APP_VERSION}.rpm"
    fi

    # AppImage (universal, sem root)
    echo "🐧 Gerando AppImage (universal Linux)..."
    jpackage \
        "${COMMON_ARGS[@]}" \
        --name  "${APP_NAME}" \
        --type  app-image
    echo "✅ Linux app-image: dist/${APP_NAME}/"

else
    echo "❌ OS não suportado por este script: $OS"
    echo "   Use build-windows.ps1 no Windows."
    exit 1
fi

echo ""
echo "📦 Instaladores gerados em: $(pwd)/dist/"
ls -lh dist/
