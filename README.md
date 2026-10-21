# AutoWizard - Vehicle Management System

A complete, modern vehicle management mobile application built with React Native, TypeScript, and Redux Toolkit, integrated with a Spring Boot backend.

## 🚀 Features

- **Vehicle Management** - Add, edit, and manage your vehicle fleet
- **Maintenance Tracking** - Schedule and track maintenance tasks
- **Smart Notifications** - Get reminders for upcoming maintenance
- **User Authentication** - Secure login and registration system
- **Modern UI/UX** - Clean, responsive design with smooth animations
- **Real-time Sync** - Integrated with Spring Boot backend API
- **Cross-platform** - Works on both iOS and Android

## 🛠 Tech Stack

- **Frontend**: React Native 0.72.6
- **Language**: TypeScript 4.8.4
- **State Management**: Redux Toolkit
- **Navigation**: React Navigation 6.x
- **Styling**: Styled Components
- **HTTP Client**: Axios
- **Storage**: AsyncStorage
- **Icons**: React Native Vector Icons
- **Backend**: Spring Boot 3.2.3

## 📱 Screens

### Authentication
- **Login Screen** - User authentication with email/password
- **Register Screen** - New user registration

### Main App
- **Home Dashboard** - Overview of vehicles and maintenance tasks
- **Vehicle List** - View and manage all vehicles
- **Vehicle Details** - Detailed vehicle information and actions
- **Add Vehicle** - Register new vehicles to the fleet
- **Maintenance Tasks** - View and manage maintenance tasks
- **Add Maintenance Task** - Schedule new maintenance tasks
- **Task Details** - Detailed task information and status updates
- **Profile** - User profile and app settings

## 🏗 Project Structure

```
src/
├── api/                 # API client and service functions
│   ├── apiClient.ts     # Axios configuration
│   ├── authApi.ts       # Authentication API calls
│   ├── vehicleApi.ts    # Vehicle management API calls
│   └── maintenanceApi.ts # Maintenance tasks API calls
├── components/          # Reusable UI components
│   └── navigation/      # Navigation components
├── navigation/          # Navigation configuration
│   └── AppNavigator.tsx # Main navigation setup
├── screens/             # App screens
│   ├── auth/           # Authentication screens
│   └── main/           # Main app screens
├── state/              # Redux state management
│   ├── store.ts        # Redux store configuration
│   └── slices/         # Redux slices
│       ├── authSlice.ts
│       ├── vehicleSlice.ts
│       └── maintenanceSlice.ts
├── styles/             # Global styles and theme
│   └── theme.ts        # App theme configuration
└── utils/              # Utility functions
```

## 🚀 Quick Start

### Prerequisites

- **Node.js** (v18.15.0 or higher)
- **npm** (v9.5.0 or higher)
- **React Native CLI** (globally installed)
- **Android Studio** (for Android development)
- **Xcode** (for iOS development, macOS only)
- **Java Development Kit (JDK)** 11 or higher
- **Android SDK** (API level 33 or higher)

### Installation

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd AutoWizardApp
   ```

2. **Install dependencies:**
   ```bash
   npm install --legacy-peer-deps
   ```

3. **Start the backend server:**
   ```bash
   # Navigate to backend directory and run Spring Boot app
   cd ../backend
   ./mvnw spring-boot:run
   ```

4. **Start the React Native app:**
   ```bash
   # For Android
   npm run android
   
   # For iOS
   npm run ios
   ```

## 🔧 Configuration

### Backend API Configuration

The app is configured to connect to a Spring Boot backend running on `http://localhost:8080`. Update the API base URL in `src/api/apiClient.ts` if needed:

```typescript
const API_BASE_URL = 'http://localhost:8080';
```

### Environment Setup

For production deployment, consider using environment variables for API endpoints and other configuration.

## 📊 State Management

The app uses Redux Toolkit for state management with the following slices:

- **Auth Slice** - User authentication state
- **Vehicle Slice** - Vehicle data and operations
- **Maintenance Slice** - Maintenance tasks and scheduling

## 🎨 Theme System

The app uses a comprehensive theme system with:
- **Colors** - Primary, secondary, success, warning, error colors
- **Typography** - Consistent font sizes and weights
- **Spacing** - Standardized spacing values
- **Shadows** - Elevation and shadow effects

## 🔐 Security

- **JWT Authentication** - Secure token-based authentication
- **Token Storage** - Secure token storage using AsyncStorage
- **API Security** - Automatic token injection in API requests
- **Error Handling** - Comprehensive error handling and user feedback

## 📱 Platform Support

- **iOS** - iOS 12.0 and later
- **Android** - Android 5.0 (API level 21) and later

## 🧪 Testing

Run the test suite:
```bash
npm test
```

## 📦 Building for Production

### Android
```bash
cd android
./gradlew assembleRelease
```

### iOS
```bash
cd ios
xcodebuild -workspace AutoWizardApp.xcworkspace -scheme AutoWizardApp -configuration Release
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

## 🔄 Version History

- **v1.0.0** - Initial release with core vehicle management features
- Integrated with Spring Boot backend
- Complete authentication system
- Vehicle and maintenance management
- Modern UI/UX design

---

**AutoWizard** - Keep your vehicles in top condition with smart maintenance tracking! 🚗🔧 