# 🐛 BugFood

> Monitor automático de pedidos para entregadores iFood — captura nomes e códigos automaticamente via Accessibility Service.

---

## Como gerar o APK (sem precisar de computador)

### 1. Fork / Upload este repositório no GitHub
- Crie conta em [github.com](https://github.com)
- Clique em **"+"** → **New repository** → Nome: `bugfood`
- Upload dos arquivos deste ZIP

### 2. O APK é gerado automaticamente
Toda vez que você enviar código ao repositório, o **GitHub Actions** compila o APK automaticamente.

- Vá em **Actions** → **Build BugFood APK** → clique no último run
- Em **Artifacts** → baixe `BugFood-debug-apk`

---

## Instalação no celular

1. Baixe o `app-debug.apk`
2. No Android: **Configurações → Segurança → Instalar apps desconhecidos** → permitir para o navegador/gerenciador de arquivos
3. Abra o APK e instale
4. Após instalar: **Configurações → Acessibilidade → BugFood → Ativar**

---

## Funcionalidades

- 🔍 **Captura automática** de nome e código de coleta do iFood
- ⚡ **Auto-fill** do código de entrega quando o mesmo cliente aparece novamente
- 🗄️ **Banco de dados** local com todos os pedidos
- ☀️🌙 **Tema dia/noite** com alternância pelo menu
- 🔔 **Notificações** de captura e auto-preenchimento
- 🔄 **Inicia automaticamente** após reboot do celular

---

## Requisitos

- Android 8.0+ (API 26)
- iFood Entregadores instalado
- Serviço de Acessibilidade ativado para o BugFood

---

## Estrutura

```
app/src/main/java/com/bugfood/
├── database/       # Room DB (DeliveryEntity, DAO, AppDatabase)
├── service/        # AccessibilityService + ForegroundService + BootReceiver
├── ui/             # MainActivity + Adapter + ViewModel
└── utils/          # NotificationHelper
```
