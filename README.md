# Bloqueador de Reels do Instagram + Lista de Planejamento

Esse app nasceu quando eu vi meu irmão e meus amigos presos no loop infinito dos Reels. Sabe aquela sensação de "brain rot"? De rolar vídeo curto sem parar, perder a noção do tempo, e no fim do dia perceber que passou horas vendo conteúdo que você nem lembra? Pois é. A dopamina fácil destruindo qualquer chance de foco.

Eu sou desenvolvedor Android e pensei: já que o problema é digital, a solução também pode ser. Foi aí que comecei a criar este app — algo que não só bloqueia os Reels e Shorts, mas também organiza o dia com um planejador de hábitos de verdade.

---

## O que faz

**Agenda semanal de bloqueio (por app, por dia, por horário):**
- Para cada app monitorado (Instagram e YouTube), cada dia da semana tem sua própria regra
- **Abertura do app:** liberada o dia todo, bloqueada o dia todo, ou liberada só em faixas de horário — quantas faixas você quiser por dia
- **Reels/Shorts:** bloqueados, liberados, ou liberados até uma cota de minutos definida para aquele dia
- Exemplo real: Instagram só abre de 18:00 às 20:00 na terça, não abre de jeito nenhum na quarta, e no sábado abre o dia todo com 30 min de Reels

**Planejador de hábitos completo (RoutineTracker):**
- Criação de rotinas personalizadas com schedules variados (diário, semanal, mensal, dias alternados)
- Acompanhamento de streaks (sequências) com calendário visual
- Metas por período e notificações de lembrete

## Funcionalidades em detalhe

| Funcionalidade | Descrição |
|---|---|
| **Agenda por dia da semana** | Cada dia tem sua própria configuração, independente dos outros |
| **Faixas de horário** | Várias janelas por dia; faixas encostadas ou sobrepostas são unidas automaticamente |
| **Cota diária por dia** | Limite de minutos de Reels/Shorts diferente para cada dia da semana |
| **Contagem por app** | Instagram e YouTube têm contadores de consumo separados |
| **Copiar configuração** | Aplica o dia atual em todos os dias, só nos dias úteis ou só no fim de semana |
| **Chave geral** | Liga e desliga toda a proteção sem perder nenhuma configuração |
| **Pausa temporária** | Pausa de 15, 30, 60 ou 120 minutos com contagem regressiva |
| **Ação ao bloquear** | Voltar uma tela (sai do Reels) ou ir direto para a tela inicial |
| **Aviso personalizado** | Mensagem própria exibida no momento do bloqueio |
| **Modo rígido** | Impede desligar ou pausar enquanto um bloqueio de horário está valendo |
| **Proteção por senha** | Trava as configurações (SHA-256), com tempo de desbloqueio configurável |
| **Device Admin** | Dificulta a desinstalação por impulso — pode ser ativado e desativado pelo app |
| **Planejador de rotinas** | Crie hábitos, acompanhe streaks, visualize calendário de conclusões |

## Como funciona (tecnicamente)

### Arquitetura
- **Multi-module** com convention plugins do Gradle para reuso de configuração
- **Koin** para injeção de dependência
- **SQLDelight** para persistência local dos dados de hábitos e streaks
- **DataStore Preferences** para as agendas e configurações do bloqueador

### Bloqueio (feature/shortsblocker)
- Um `AccessibilityService` monitora a janela ativa do Android
- As regras vivem em `models/BlockRules.kt`: `AppSchedule` (um app) → `DaySchedule` (um dia) → `TimeWindow` (uma faixa de horário). A avaliação é pura, sem dependência de Android, e coberta por testes
- A agenda é serializada por `utils/ScheduleCodec.kt` em uma string compacta por app, sem precisar de biblioteca de serialização
- O serviço avalia duas camadas em ordem: primeiro a **abertura do app** (dia bloqueado ou fora da janela), depois o **conteúdo curto** (política do dia e cota consumida)
- Detectores específicos (`InstagramReelsDetector`, `YouTubeShortsDetector`) identificam quando uma tela de conteúdo curto está aberta
- Um heartbeat a cada 20s conta os minutos consumidos e reavalia a agenda — é ele que tira o usuário do app quando a janela de horário termina, mesmo sem novos eventos de acessibilidade
- Ao bloquear, o serviço executa `GLOBAL_ACTION_BACK` ou `GLOBAL_ACTION_HOME`, conforme a configuração

### Interface
- Barra inferior com duas seções: **Bloqueio** e **Planejamento** (a antiga tela intermediária de escolha foi removida)
- Tela inicial do bloqueio: estado da proteção, pausa rápida e um cartão por app com o que está valendo agora
- Tela de agenda por app: seletor de dia, editor do dia e resumo da semana inteira
- Tela de ajustes: ação ao bloquear, aviso, senha, modo rígido, device admin e serviço
- Tema próprio (indigo + verde-água), cantos arredondados e suporte a modo claro/escuro

### Planejador (features agenda, addeditroutine, routinedetails)
- Baseado no projeto **RoutineTracker**
- Interface em Jetpack Compose com tema próprio e suporte a calendário
- Camadas: `data` (repositórios), `database` (SQLDelight + mappers), `domain` (use cases), `model` (entidades Kotlin)

### Segurança
- Senha armazenada como hash SHA-256 no DataStore
- O desbloqueio dura 1, 5, 15 ou 30 minutos (configurável), depois as configurações travam de novo
- `AdminReceiver` (DeviceAdminReceiver) registrado no AndroidManifest com política de dispositivo

### Migração
Quem já usava a versão anterior não perde nada: as configurações antigas (dias liberados, cota única, dias de bloqueio total) são convertidas automaticamente para a agenda semanal na primeira abertura.

## Como buildar

```bash
./gradlew.bat :app:assembleDebug
```

O APK gerado estará em `app/build/outputs/apk/debug/app-debug.apk`.

Para rodar os testes das regras de bloqueio:

```bash
./gradlew.bat :feature:shortsblocker:testDebugUnitTest
```

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
