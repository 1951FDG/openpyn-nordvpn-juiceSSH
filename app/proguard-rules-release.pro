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
