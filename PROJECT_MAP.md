# Project Map

## Core Structure
- / (Root)
  - CLAUDE.md          # Governance & Rules
  - PROJECT_MAP.md     # This file (Map)
  - PROGRESS.md        # State Machine & Tasks
  - INFO.md            # Temp Buffer (Logs/Specs)
  - DECISIONS.md       # Architectural Log

## Application Files
- app/src/main/java/com/projektt/app/MainActivity.kt: Main activity and UI entry point.
- app/src/main/java/com/projektt/app/ProjektTApp.kt: Application class and Hilt setup.
- app/src/main/java/com/projektt/app/ui/: UI layer (Screens, ViewModels, Components).
- app/src/main/java/com/projektt/app/ui/components/TaskDialogs.kt: AddTaskDialog, EditTaskDialog, QuickDateChip.
- app/src/main/java/com/projektt/app/ui/screens/SettingsScreen.kt: Widget-Listen Settings UI.
- app/src/main/java/com/projektt/app/ui/screens/SettingsViewModel.kt: Settings ViewModel.
- app/src/main/java/com/projektt/app/domain/: Domain layer (UseCases, Models).
- app/src/main/java/com/projektt/app/data/: Data layer (Repositories, Data Sources).
- app/src/main/java/com/projektt/app/data/local/WidgetSettingsEntity.kt: Widget-Listen Settings Entity.
- app/src/main/java/com/projektt/app/data/local/WidgetSettingsDao.kt: Widget-Listen Settings DAO.
- app/src/main/java/com/projektt/app/data/repository/WidgetSettingsRepository.kt: Widget Settings Repository.
- app/src/main/java/com/projektt/app/di/: Dependency Injection modules (Hilt).
- app/src/main/java/com/projektt/app/widget/: Glance widget implementation.

## Decisions Index
- Brain/decisions/*.md : Architektur-Entscheidungen (Immer prüfen, wenn Kernlogik geändert wird)