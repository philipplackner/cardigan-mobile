# Spec Delta

## Purpose

Defines how the app's shared UI looks and behaves on Android and iOS across light and dark themes, window sizes and input methods, so every screen built from it matches the Figma designs.

## ADDED Requirements

### Requirement: Theme follows the system setting
The app SHALL use the light theme when the system is in light mode and the dark theme when it is in dark mode, and SHALL switch when the system setting changes while the app is open. The app SHALL NOT offer an in-app theme setting and SHALL NOT use dynamic system colors on Android.

#### Scenario: System switches to dark
- **WHEN** the user switches the system to dark mode while the app is open
- **THEN** every component shows its Figma Dark mode colors without restarting the app

#### Scenario: Android wallpaper colors
- **WHEN** the app runs on an Android device with dynamic color enabled
- **THEN** the app still shows the Figma colors

### Requirement: Window size classes
The app SHALL classify the window by its width as phone (under 600dp), tablet (600dp to 839dp) or desktop (840dp and up). A window under 480dp high SHALL be phone regardless of width. The app SHALL reclassify the window whenever its size changes. Layouts SHALL be chosen from this class, never from the device type or platform.

#### Scenario: Tablet portrait
- **WHEN** the app runs in a window 834dp wide
- **THEN** it is classified as tablet

#### Scenario: Tablet rotated to landscape
- **WHEN** a tablet window grows from 834dp to 1194dp wide
- **THEN** it is reclassified as desktop

#### Scenario: Split screen on a tablet
- **WHEN** the app runs in a 500dp wide split-screen window on a tablet
- **THEN** it is classified as phone

#### Scenario: Phone in landscape
- **WHEN** a phone window is 844dp wide and 390dp high
- **THEN** it is classified as phone

#### Scenario: Wide and exactly 480dp high
- **WHEN** a window is 900dp wide and 480dp high
- **THEN** it is classified as desktop

### Requirement: Component size follows the window class
Components that have Default and Touch sizes in Figma SHALL use the Touch size in the phone class and the Default size in the tablet and desktop classes.

#### Scenario: Button on a phone
- **WHEN** a primary button is shown in the phone class
- **THEN** it has the Touch height of 44

#### Scenario: Button on a tablet
- **WHEN** the same button is shown in the tablet class
- **THEN** it has the Default (Medium) height of 34

#### Scenario: Button on a phone in landscape
- **WHEN** the button is shown in a window 844dp wide and 390dp high
- **THEN** it has the Touch height of 44

### Requirement: Text areas grow with their content
Title and Multi-line text areas SHALL grow in height as their text grows, without a maximum. A Description text area SHALL grow as its text grows, and once it is taller than the space left in its container, the container SHALL scroll.

#### Scenario: Long quick-add text
- **WHEN** the user types five lines into a Multi-line text area
- **THEN** the field is five lines tall and all text is visible without scrolling inside the field

#### Scenario: Long description
- **WHEN** the description grows taller than the space left in the card detail body
- **THEN** the body scrolls and the field keeps growing

### Requirement: Progress dialog can't be dismissed
While a Progress dialog shows, the system back gesture or button, the Escape key and a tap on the scrim SHALL do nothing. Default dialogs SHALL keep the platform dismiss behavior.

#### Scenario: Back during progress
- **WHEN** a Progress dialog shows and the user does the Android back gesture
- **THEN** the dialog stays and nothing behind it changes

#### Scenario: Scrim tap during progress
- **WHEN** a Progress dialog shows and the user taps the scrim
- **THEN** the dialog stays

### Requirement: Minimum touch area
Every interactive component SHALL accept touches in an area of at least 48dp by 48dp, while keeping the visual size from Figma. Where the touch areas of neighboring components overlap, a touch SHALL go to the nearest component.

#### Scenario: Small icon button
- **WHEN** a Small icon button (visually 28dp) is shown in a column header
- **THEN** a touch 8dp outside its visible edge still activates it, and its visible size stays 28dp

#### Scenario: Dense menu on a tablet
- **WHEN** two Default menu items (36dp high) are stacked and the user touches near their shared edge
- **THEN** exactly one item activates: the one whose visible bounds are nearest to the touch

### Requirement: Press feedback
Every interactive component SHALL show a ripple while pressed. The ripple color SHALL be `text/primary` on neutral surfaces and `text/on-accent` on accent and danger fills, from the active theme.

#### Scenario: Pressing a ghost button in dark mode
- **WHEN** the user presses a Ghost button in the dark theme
- **THEN** a ripple in the dark `text/primary` color shows while the finger is down

#### Scenario: Pressing a primary button in light mode
- **WHEN** the user presses a Primary button in the light theme
- **THEN** the ripple uses the light `text/on-accent` color

### Requirement: Hover and keyboard highlight
Components with a Highlighted state in Figma SHALL show it while a pointer hovers over them or while they have keyboard focus. They SHALL NOT show it for touch input alone.

#### Scenario: Trackpad hover on a menu item
- **WHEN** a trackpad pointer hovers over a menu item on a tablet
- **THEN** the item shows its Highlighted look

#### Scenario: Keyboard navigation in the label filter
- **WHEN** the user moves keyboard focus to an option in the label filter
- **THEN** the option shows its Highlighted look

#### Scenario: Touch only
- **WHEN** the user taps an option with a finger
- **THEN** the option shows the press ripple, not the Highlighted look

### Requirement: Label palette
The design system SHALL provide exactly 20 label colors, named as in the shared `domain-label` palette, each with a light and a dark value from Figma.

#### Scenario: Label color in dark mode
- **WHEN** a label with the color `Teal` is shown in the dark theme
- **THEN** its dot uses the dark value of `label/teal`

### Requirement: Phone text sizes
The Page title, Card title and Body text roles SHALL use their larger phone sizes from Figma Foundations in the phone class, and their regular sizes in the tablet and desktop classes.

#### Scenario: Card title on a phone
- **WHEN** a card title is shown in the phone class
- **THEN** it is 14.5 in size, and 13.5 in the tablet class

### Requirement: Toast timing
A success toast SHALL close on its own 4 seconds (±0.5s) after it appears, or after the longer timeout the platform's accessibility settings recommend (Android "Time to take action"). On iOS it SHALL close after 4 seconds (±0.5s). An error toast SHALL stay until the user closes it. Every toast SHALL offer a close control. Only one toast SHALL show at a time, and a new toast SHALL replace the one showing.

#### Scenario: Success toast
- **WHEN** a success toast appears and the user does nothing
- **THEN** it closes 4 seconds (±0.5s) after it appeared

#### Scenario: Android accessibility timeout
- **WHEN** Android's "Time to take action" is set to 1 minute and a success toast appears
- **THEN** it stays for 1 minute

#### Scenario: Error toast
- **WHEN** an error toast appears and the user does nothing for a minute
- **THEN** it is still showing

#### Scenario: Closing an error toast
- **WHEN** the user activates the close control of an error toast
- **THEN** the toast closes

#### Scenario: Second toast while one shows
- **WHEN** an error toast is showing and a success toast is triggered
- **THEN** the error toast is replaced by the success toast, and the success toast closes 4 seconds (±0.5s) after it appeared

### Requirement: Skeleton shimmer
Skeleton placeholders SHALL shimmer between the skeleton and skeleton-highlight colors in a loop of 1.2 seconds (±0.1s). When the OS setting to reduce motion (Android "Remove animations", iOS "Reduce Motion") is on, skeletons SHALL show the skeleton color without motion.

#### Scenario: Default motion
- **WHEN** a list is loading and reduced motion is off
- **THEN** its skeletons shimmer

#### Scenario: Reduced motion
- **WHEN** a list is loading and iOS Reduce Motion is on
- **THEN** its skeletons show a still skeleton color

### Requirement: Large system font sizes
Component text SHALL scale with the system font size. Figma control heights SHALL act as minimum heights, so controls grow to fit larger text. Single-line labels that don't fit SHALL be cut off with an ellipsis instead of wrapping.

#### Scenario: Button at 200% font size
- **WHEN** the system font size is 200% and a Touch button is shown
- **THEN** its label is twice its Figma size, the button is taller than 44 to fit it, and the label is not clipped vertically

#### Scenario: Long menu item label
- **WHEN** a menu item label is wider than the menu at a large font size
- **THEN** the label stays on one line and ends with an ellipsis

### Requirement: Pull-to-refresh indicator
Where a screen offers pull to refresh, the indicator SHALL use the Material pull-to-refresh behavior with colors from the theme.

#### Scenario: Pull in dark mode
- **WHEN** the user pulls down on a refreshable list in the dark theme
- **THEN** the indicator shows on a dark theme surface with the accent color

### Requirement: Accessible names from string resources
Every interactive component that shows only an icon SHALL take a non-null `contentDescription` parameter and expose it as its accessibility label to TalkBack and VoiceOver. Callers pass a string resource.

#### Scenario: Icon button with screen reader
- **WHEN** TalkBack or VoiceOver focuses an icon button
- **THEN** it reads the button's content description
