-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

-assumenosideeffects class org.slf4j.Logger {
    public *** trace(...);
    public *** debug(...);
    public *** info(...);
    public *** warn(...);
}

-assumenosideeffects class android.util.Log {
   public static *** v(...);
   public static *** d(...);
   public static *** i(...);
   public static *** w(...);
   public static *** e(...);
   public static *** println(...);
}

-keep public class androidx.navigation.fragment.NavHostFragment { *; }

-keep public class io.requery.android.database.sqlite.SQLiteCustomFunction { *; }
-keep public class io.requery.android.database.sqlite.SQLiteDebug** { *; }
-keep public class io.requery.android.database.sqlite.SQLiteFunction { *; }

-keepclasseswithmembers class io.requery.android.database.** {
  native <methods>;
}

-keepclasseswithmembers class io.requery.android.database.sqlite.SQLiteConnection {
  native <methods>;
}

-keepclasseswithmembers class io.requery.android.database.sqlite.SQLiteGlobal {
  native <methods>;
}

-keepnames class io.requery.android.database.** { *; }
