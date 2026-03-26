# Build — Instaladores Nativos

Cada instalador embarca o JRE completo. O usuário final **não precisa ter Java instalado**.

## Requisitos comuns
- JDK 21+ (Oracle ou Temurin) — deve incluir `jpackage` e `jlink`
- Maven 3.8+

---

## macOS → `.dmg`

```bash
./build-native.sh
# Saída: dist/FinancasPessoais-1.0.0.dmg
```

Instala em `/Applications`. Compatível com macOS 11+ (Intel e Apple Silicon).

---

## Linux → `.deb` / `.rpm` / app-image

Execute na máquina Linux alvo (ou numa VM/container da distro desejada):

```bash
./build-native.sh
```

| Formato | Distros | Saída |
|---|---|---|
| `.deb` | Ubuntu, Debian, Mint, Pop!_OS | `dist/financas-pessoais_1.0.0_amd64.deb` |
| `.rpm` | Fedora, RHEL, openSUSE | `dist/financas-pessoais-1.0.0-1.x86_64.rpm` |
| `app-image` | Qualquer Linux (sem root) | `dist/FinancasPessoais/` |

**Instalar .deb:**
```bash
sudo dpkg -i dist/financas-pessoais_1.0.0_amd64.deb
```

**Instalar .rpm:**
```bash
sudo rpm -i dist/financas-pessoais-1.0.0-1.x86_64.rpm
```

**Executar app-image (sem instalar):**
```bash
dist/FinancasPessoais/bin/FinancasPessoais
```

---

## Windows → `.msi` e `.exe`

Execute no Windows com PowerShell:

```powershell
.\build-windows.ps1
# Saída: dist\FinancasPessoais-1.0.0.msi
#        dist\FinancasPessoais-Setup-1.0.0.exe
```

### Pré-requisitos Windows

| Ferramenta | Para que serve | Download |
|---|---|---|
| JDK 21 | Compilar + jpackage | https://adoptium.net |
| Maven | Build do projeto | https://maven.apache.org |
| WiX Toolset 3.x | Gerar `.msi` | https://wixtoolset.org |

> O WiX precisa estar no PATH. Após instalar, reinicie o terminal.

---

## Dados do usuário

O banco H2 fica em `./data/finance.mv.db` relativo ao diretório de execução.  
Nos instaladores nativos o jpackage define o diretório de trabalho como a pasta do app,
então os dados ficam junto ao executável e são preservados entre atualizações.
