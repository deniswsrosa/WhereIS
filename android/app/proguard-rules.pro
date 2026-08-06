# WhereIS (com.acme.clara) release keep rules.
#
# Context: the app deliberately avoids reflection-based (de)serialization — save data
# (save/SaveCodec.kt) and the i18n/humor JSON catalogs (i18n/Strings.kt, game/Humor.kt) are
# all parsed by hand into/out of Map<String, Any?> and built back into data classes via plain
# constructor calls, keyed by explicit string literals. R8 traces that as ordinary code, so it
# needs no help. The two known reflection-sensitive spots are below.

# --- Enums looked up by name at runtime -----------------------------------------------------
# save/SaveCodec.kt calls Phase.valueOf(...) and ClueKind.valueOf(...) to restore saved game
# state, and Venue/JournalEntry round-trip enum .name strings through the save JSON. R8's enum
# optimizations can strip or rename the values()/valueOf() machinery those calls rely on, which
# would corrupt or crash on loading old saves. Kept broadly (all enums in the app package) so any
# enum added later that's persisted or matched by name stays safe by default.
-keepclassmembers enum com.acme.clara.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- WorkManager worker ------------------------------------------------------------------------
# notify/WelcomeBackWorker.kt is instantiated by WorkManager via reflection (the worker class
# name is persisted in the WorkRequest and looked up by Class.forName at run time), not via a
# call graph R8 can see. androidx.work's own consumer proguard rules already keep any
# `extends androidx.work.Worker` subclass and its constructor, but we keep it explicitly too so
# this stays correct even if that transitive dependency's packaged rules ever change.
-keep public class com.acme.clara.notify.WelcomeBackWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Note: org.json.JSONObject/JSONArray (used by Strings.kt and Humor.kt) are Android platform
# classes living on the boot classpath, not app/library code — R8 never processes them, so no
# keep rule is needed for the i18n/humor JSON parsing itself. Likewise assets/i18n/*.json and
# assets/humor*.json live under assets/, which isShrinkResources never touches (only res/ is
# resource-shrunk), so the catalogs themselves can't be stripped.
