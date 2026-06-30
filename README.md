# Bloqueador de Reels do Instagram + Lista de Planejamento

Esse app nasceu quando eu vi meu irmão e meus amigos presos no loop infinito dos Reels. Sabe aquela sensação de "brain rot"? De rolar vídeo curto sem parar, perder a noção do tempo, e no fim do dia perceber que passou horas vendo conteúdo que você nem lembra? Pois é. A dopamina fácil destruindo qualquer chance de foco.

Eu sou desenvolvedor Android e pensei: já que o problema é digital, a solução também pode ser. Foi aí que comecei a criar este app — algo que não só bloqueia os Reels e Shorts, mas também organiza o dia com um planejador de hábitos de verdade.

---

## O que faz

**Bloqueio inteligente de conteúdo curto:**
- Detecta quando você está assistindo **Reels do Instagram** ou **Shorts do YouTube** usando o Serviço de Acessibilidade do Android
- Aplica regras configuráveis: cota diária de minutos, dias da semana com bloqueio parcial ou total, e até bloqueio completo de abertura do app
- **Chave geral** — desliga todo o bloqueio sem perder suas configurações

**Planejador de hábitos completo (RoutineTracker):**
- Criação de rotinas personalizadas com schedules variados (diário, semanal, mensal, dias alternados)
- Acompanhamento de streaks (sequências) com calendário visual
- Metas por período e notificações de lembrete

## Funcionalidades em detalhe

| Funcionalidade | Descrição |
|---|---|
| **Cota diária por app** | Limite de minutos para Reels e Shorts separadamente |
| **Dias de bloqueio** | Escolha quais dias da semana aplicar a cota ou bloquear totalmente |
| **App Block** | Bloqueia abertura do Instagram em dias selecionados |
| **Chave geral (master toggle)** | Ativa/desativa todo o bloqueio sem perder configurações |
| **Proteção por senha** | Impede alterações nas configurações do bloqueador (SHA-256) |
| **Device Admin** | Dificulta a desinstalação direta — exige desativação como admin primeiro |
| **Planejador de rotinas** | Crie hábitos, acompanhe streaks, visualize calendário de conclusões |

## Como funciona (tecnicamente)

### Arquitetura
- **Multi-module** com convention plugins do Gradle para reuso de configuração
- **Koin** para injeção de dependência (17 módulos)
- **SQLDelight** para persistência local dos dados de hábitos e streaks
- **DataStore Preferences** para configurações do bloqueador (dias, cota, senha, etc.)

### Bloqueio (feature/shortsblocker)
- Um `AccessibilityService` monitora a janela ativa do Android
- Detectores específicos (`InstagramReelsDetector`, `YouTubeShortsDetector`) identificam quando uma tela de conteúdo curto está aberta
- O heartbeat do serviço incrementa o contador de minutos usado a cada 60s enquanto o usuário está em um Reels/Shorts permitido
- Quando a cota do dia é atingida, o serviço executa `GLOBAL_ACTION_BACK` para forçar a saída

### Planejador (features agenda, addeditroutine, routinedetails)
- Baseado no projeto **RoutineTracker**
- Interface em Jetpack Compose com tema próprio e suporte a calendário
- Camadas: `data` (repositórios), `database` (SQLDelight + mappers), `domain` (use cases), `model` (entidades Kotlin)

### Segurança
- Senha armazenada como hash SHA-256 no DataStore
- Desbloqueio dura 5 minutos, após o qual as configurações são travadas novamente
- `AdminReceiver` (DeviceAdminReceiver) registrado no AndroidManifest com política de dispositivo

## Como buildar

```bash
./gradlew.bat :app:assembleDebug
```

O APK gerado estará em `app/build/outputs/apk/debug/app-debug.apk`.

**Pré-requisitos:**
- Android Studio (recomendado)
- JDK 17+
- Compile SDK 34, target SDK 34, minSdk 24

## Tecnologias

- **Linguagem:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Koin
- **Persistência:** SQLDelight + DataStore
- **Build:** Gradle com convention plugins (multi-module)
- **MinSdk:** 24 | **Target:** 34 | **Compile:** 34

---

**Autor:** Matheus Mendes

Feito com o objetivo de ajudar quem quer recuperar o foco e o tempo perdido em redes sociais.
