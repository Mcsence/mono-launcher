# Lumen — launcher Android noir & blanc + 1 couleur

Launcher minimaliste écrit de zéro en Kotlin + Jetpack Compose
(anciennement « Mono » ; l'identifiant `com.mono.launcher` est conservé pour les mises à jour).

Polices intégrées (Space Grotesk, Manrope, Syne, Unbounded) : Google Fonts, licence SIL Open Font License 1.1.

## Fonctionnalités

- **Accueil** : grande horloge, date, et tes apps favorites
  - style **Liste** (gros texte gras, une app par ligne)
  - ou style **Tuiles** (grille 2 colonnes, la 1re tuile dans la couleur d'accent)
- **Tiroir d'apps** (glisser vers le haut ou « Toutes les apps ») :
  - recherche instantanée (Entrée = lance la 1re app trouvée)
  - apps groupées par lettre
  - **barre alphabétique** à droite : glisser le doigt dessus pour sauter à une lettre
- **Appui long** sur une app : ajouter/retirer des favoris, monter/descendre, infos, désinstaller
- **Réglages** : couleur d'accent (6 choix), style des favoris, icônes (aucune / N&B / couleur),
  taille du texte, horloge affichée ou masquée, bouton « Définir comme écran d'accueil »

## Structure

```
app/src/main/java/com/mono/launcher/
  MainActivity.kt        activité HOME, chargement des apps
  AppRepository.kt       liste des apps (LauncherApps), lancement, désinstallation
  Prefs.kt               réglages + favoris (SharedPreferences)
  ui/Theme.kt            palette noir/blanc + couleur d'accent
  ui/LauncherRoot.kt     navigation accueil / tiroir / réglages
  ui/HomeScreen.kt       horloge + favoris (liste ou tuiles)
  ui/AppDrawer.kt        tiroir d'apps + recherche
  ui/AlphabetScroller.kt barre A–Z
  ui/SettingsScreen.kt   réglages
  ui/Components.kt       éléments partagés (icône, menu appui long, etc.)
```

## Mises à jour automatiques (GitHub + Obtainium)

Chaque push sur `main` lance GitHub Actions (`.github/workflows/release.yml`) :
l'APK est compilé, signé avec la clé de release (secrets `MONO_KEYSTORE_*`)
et publié dans une Release `v1.0.<numéro>`.

Sur le téléphone, l'app **Obtainium** suit ce dépôt et installe chaque nouvelle
Release. Comme le dépôt est privé, Obtainium a besoin d'un jeton GitHub
(fine-grained, lecture seule sur ce dépôt : *Contents: Read*).

La clé de signature est sur le PC dans `%USERPROFILE%\.android\mono-release.jks`
(mot de passe dans `mono-release.properties`). Ne la perds pas : sans elle,
plus aucune mise à jour ne pourra s'installer par-dessus l'app existante.

## Compiler en local

Les builds locaux s'appellent « Mono dev » et s'installent à côté de la version GitHub.

Avec Android Studio : *Open* → ce dossier → bouton ▶ (téléphone branché) ou
*Build › Build APK(s)*.

En ligne de commande (JDK 17 + SDK Android installés) :

```
gradlew assembleDebug
```

L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
Copie-le sur le téléphone, ouvre-le pour l'installer, puis appuie sur Home et
choisis **Mono** comme écran d'accueil.
