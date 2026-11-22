# Personal Hisab Khata — React + TypeScript

বাংলা ভাষা, Blue & White theme এবং BDT (৳) মুদ্রায় ব্যক্তিগত আয়-খরচ ব্যবস্থাপনা।

## Google AI Studio-তে ব্যবহার
1. https://aistudio.google.com/ খুলে **Build** নির্বাচন করুন।
2. এই প্রজেক্টের ZIP ফাইল আপলোড করুন (অথবা ফাইলগুলো project-এ যোগ করুন)।
3. AI Studio-কে বলুন: “এই React + TypeScript প্রজেক্টটি রান করাও, error ঠিক করো এবং UI/সব feature সম্পূর্ণ করো। বিদ্যমান ফিচার নষ্ট করবে না।”
4. Build/Preview দিয়ে অ্যাপ চালান।

## লোকালি চালানো
Node.js 20+ লাগবে।
```bash
npm install
npm run dev
```
Production build:
```bash
npm run build
```

## Firebase Cloud Sync সেটআপ
1. Firebase Console-এ একটি project তৈরি করুন।
2. Authentication → Sign-in method → Email/Password enable করুন।
3. Firestore Database তৈরি করুন।
4. `.env.example` কপি করে `.env` করুন এবং Firebase Web App config বসান।
5. Firestore Rules নিচের মতো দিন:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/transactions/{transactionId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
6. App-এ Settings থেকে account create/login করুন।

**সতর্কতা:** Firebase credentials যোগ না করা পর্যন্ত অ্যাপ লোকাল-স্টোরেজে কাজ করবে। এই সংস্করণে Firebase sign-in এবং signed-in user-এর transaction load/save/delete আছে। পূর্ণ বহু-ডিভাইস conflict resolution, robust offline queue এবং server-side backup এখনো implement করা হয়নি।

## Android APK
Capacitor যোগ করে:
```bash
npm install @capacitor/core @capacitor/cli
npm install @capacitor/android
npx cap init "Personal Hisab Khata" "com.personalhisabkhata.app" --web-dir=dist
npm run build
npx cap add android
npx cap sync android
npx cap open android
```
Android Studio-তে Build → Build APK(s)।
