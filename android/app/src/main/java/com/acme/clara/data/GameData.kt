// AUTO-GENERATED from corpus/game_data.json by scripts/gen_kotlin_data.py
// Byte-exact strings extracted from CARMEN.EXE (Enhanced, MS-DOS v2.1, (c)1990 Broderbund).
// DO NOT EDIT BY HAND.
package com.acme.clara.data

data class Suspect(
    val name: String, val sex: String, val occupation: String,
    val hobby: String, val hair: String, val auto: String,
    val feature1: String, val feature2: String,
    val tSex: String, val tHobby: String, val tHair: String,
    val tFeature: String, val tVehicle: String,
)

object GameData {
    val cities = listOf(
        "Athens",
        "Baghdad",
        "Bamako",
        "Bangkok",
        "Budapest",
        "Buenos Aires",
        "Cairo",
        "Colombo",
        "Istanbul",
        "Kathmandu",
        "Kigali",
        "Lima",
        "London",
        "Mexico City",
        "Montreal",
        "Moroni",
        "Moscow",
        "New Delhi",
        "New York",
        "Oslo",
        "Paris",
        "Beijing",
        "Port Moresby",
        "Reykjavik",
        "Rio de Janeiro",
        "Rome",
        "San Marino",
        "Singapore",
        "Sydney",
        "Tokyo"
    )

    val hobbies = listOf(
        "tennis",
        "music",
        "mt. climbing",
        "skydiving",
        "swimming",
        "croquet"
    )
    val hairColors = listOf(
        "brown",
        "blond",
        "red",
        "black"
    )
    val features = listOf(
        "limps",
        "ring",
        "tattoo",
        "scar",
        "jewelry"
    )
    val vehicles = listOf(
        "convertible",
        "limousine",
        "race car",
        "motorcycle"
    )
    val sexes = listOf("female", "male")

    val venues = listOf(
        "Bank",
        "Hotel",
        "Museum",
        "Sport Club",
        "Library",
        "Airport",
        "Harbor",
        "Riverfront",
        "Palace",
        "Stock Exchange",
        "Marketplace",
        "Foreign Ministry"
    )
    val occupations = listOf(
        "Vice President",
        "Bank Guard",
        "Teller",
        "Hotel manager",
        "Bellhop",
        "House detective",
        "Museum guard",
        "Docent",
        "Curator",
        "Waiter",
        "Tennis pro",
        "Bartender",
        "Circulation clerk",
        "Reference librarian",
        "Archivist",
        "Pilot",
        "Flight attendant",
        "Baggage clerk",
        "Sailor",
        "Harbor Master",
        "Customs officer",
        "Stevedore",
        "Tugboat captain",
        "Sailor's parrot",
        "Palace guard",
        "Soldier",
        "Privy Councillor",
        "Analyst",
        "Trader",
        "Messenger",
        "Hawker",
        "Street merchant",
        "Urchin",
        "Under Secretary",
        "Attache",
        "Ambassador"
    )
    // Which witnesses staff each venue (verified against DOSBox captures).
    val venueOccupations = mapOf(
        "Bank" to listOf("Vice President", "Bank Guard", "Teller"),
        "Hotel" to listOf("Hotel manager", "Bellhop", "House detective"),
        "Museum" to listOf("Museum guard", "Docent", "Curator"),
        "Sport Club" to listOf("Tennis pro", "Waiter", "Bartender"),
        "Library" to listOf("Circulation clerk", "Reference librarian", "Archivist"),
        "Airport" to listOf("Pilot", "Flight attendant", "Baggage clerk"),
        "Harbor" to listOf("Sailor", "Harbor Master", "Customs officer", "Stevedore", "Tugboat captain"),
        "Riverfront" to listOf("Sailor", "Stevedore", "Urchin", "Tugboat captain"),
        "Palace" to listOf("Palace guard", "Soldier", "Privy Councillor"),
        "Stock Exchange" to listOf("Analyst", "Trader", "Messenger"),
        "Marketplace" to listOf("Hawker", "Street merchant", "Urchin"),
        "Foreign Ministry" to listOf("Under Secretary", "Attache", "Ambassador"),
    )
    // Off-track witnesses answer with their venue's own line (DOS keeps one per
    // venue, in venue-list order in the EXE; several venues share the apology).
    val noInformationByVenue = mapOf(
        "Bank" to "I'm sorry, I have never seen the person you are looking for.",
        "Hotel" to "No one like that has checked in here.",
        "Museum" to "I'm sorry, I have never seen the person you are looking for.",
        "Sport Club" to "Sorry, I haven't seen anybody like that around here.",
        "Library" to "I don't think I've seen anybody like that around the library.",
        "Airport" to "It's awfully busy around here; I haven't noticed anyone suspicious.",
        "Harbor" to "Sorry, I haven't noticed anything suspicious around the harbor.",
        "Riverfront" to "There hasn't been another person around here all day.",
        "Palace" to "I'm sorry, I have never seen the person you are looking for.",
        "Stock Exchange" to "No one like that has done business here today.",
        "Marketplace" to "Sorry, I haven't seen anybody like that around here.",
        "Foreign Ministry" to "I'm sorry, I have never seen the person you are looking for.",
    )
    val noInformation = noInformationByVenue.values.distinct()
    val clueLeadIns = listOf(
        "My sources tell me",
        "A reliable source told me",
        "I heard",
        "The person you're looking for was here and",
        "Sources tell me",
        "I saw the person you're looking for and",
        "All I know is that",
        "A suspicious person was here and",
        "Yes",
        "No"
    )
    val dangerMessages = listOf(
        "All I can say is that something shady is happening around town.",
        "Word on the street says the gang is hiding somewhere in town.",
        "The whisper going around: you're closing in, snoop...",
        "My only advice: mind where you walk!",
        "Yes",
        "No",
        ":"
    )
    // Ranks 0..4 are the free career (Progression.FREE_RANKS). Ranks 5..14 are the paid
    // "International" grades — one per recognition wave (see data/Progression.kt).
    val ranks = listOf(
        "Rookie",
        "Sleuth",
        "Private Eye",
        "Investigator",
        "Ace Detective",
        "Special Agent",
        "Field Inspector",
        "Senior Inspector",
        "Inspector",
        "Chief Inspector",
        "Superintendent",
        "Commander",
        "Deputy Director",
        "Director",
        "Chief Director"
    )

    // Suspect-trait sentences verbatim from the DOS EXE. Pronoun slots {S}/{s}/{p}
    // are filled from the culprit's sex, so a clue leaks the sex like the original.
    val traitClueFragments = mapOf(
        "hobby:tennis" to listOf("{S} said that {s} enjoyed playing tennis", "{S} asked about the recent tennis match", "{S} was carrying a tennis raquet"),
        "hobby:mt. climbing" to listOf("{S} said {s} was a mountain climber", "{S} bragged about dangerous sports", "{S} talked about great mountains"),
        "hobby:croquet" to listOf("{S} mentioned that {s} plays croquet", "{S} said {s} hated dangerous sports", "{S} talked about a croquet match"),
        "hair:brown" to listOf("{S} had brown hair"),
        "hair:blond" to listOf("{S} had blond hair"),
        "hair:red" to listOf("{S} had red hair"),
        "hair:black" to listOf("{S} had black hair"),
        "feature:ring" to listOf("{S} had a large ring on", "{S} had a fancy ring on", "I liked the ring {s} had on"),
        "feature:tattoo" to listOf("I noticed a tattoo on {p} arm", "{S} tried to conceal a tattoo", "{S} had an ugly tattoo"),
        "feature:jewelry" to listOf("{S} wore fancy jewelry", "The jewelry {s} wore was stunning", "The jewelry {s} wore looked expensive"),
        "vehicle:convertible" to listOf("{S} arrived in a convertible", "{S} had a nice convertible", "{S} offered me a ride in {p} convertible"),
        "vehicle:limousine" to listOf("{S} arrived in a private limo", "{S} was driving a limo", "{S} had {p} driver along"),
        "vehicle:motorcycle" to listOf("{S} arrived on a motorcycle", "{S} was riding a motorbike", "{S} was carrying a helmet"),
    )
    val dossierMenuNames = listOf(
        "Clara San Diego",
        "Merey LaRoc",
        "Dazzle Annie",
        "Lady Agatha",
        "Len Bulk",
        "Scar Graynolt",
        "Nick Brunch",
        "Fast Eddie B",
        "Ihor Ihorovich",
        "Katherine Drib"
    )

    val suspects = listOf(
        Suspect(
            name = "Clara San Diego", sex = "Female",
            occupation = "Once a secret agent for Monaco's intelligence bureau",
            hobby = "Tennis", hair = "Reddish-brown", auto = "1939 Packard convertible",
            feature1 = "Won't be seen anywhere without her ruby necklace.", feature2 = "Adores tacos above all foods.",
            tSex = "female", tHobby = "tennis", tHair = "red",
            tFeature = "jewelry", tVehicle = "convertible",
        ),
        Suspect(
            name = "Merey LaRoc", sex = "Female",
            occupation = "Self-employed aerobics performer",
            hobby = "Mountain climbing", hair = "Brown", auto = "Fancy limousine",
            feature1 = "Utterly obsessed with expensive jewelry.", feature2 = "Craves fiery, spicy dishes.",
            tSex = "female", tHobby = "mt. climbing", tHair = "brown",
            tFeature = "jewelry", tVehicle = "limousine",
        ),
        Suspect(
            name = "Dazzle Annie Nonker", sex = "Female",
            occupation = "Runs her own yogurt bar",
            hobby = "Tennis", hair = "Blond", auto = "Bugatti Limousine",
            feature1 = "Said to carry a tattoo.", feature2 = "Can never get enough shellfish.",
            tSex = "female", tHobby = "tennis", tHair = "blond",
            tFeature = "tattoo", tVehicle = "limousine",
        ),
        Suspect(
            name = "Lady Agatha Wayland", sex = "Female",
            occupation = "Devours high-society English detective novels",
            hobby = "Tennis", hair = "Red", auto = "Denghby Roadster",
            feature1 = "Wears a diamond ring as big as a grapefruit.", feature2 = "Races around the countryside hunting for good Mexican food.",
            tSex = "female", tHobby = "tennis", tHair = "red",
            tFeature = "ring", tVehicle = "convertible",
        ),
        Suspect(
            name = "Len \"Red\" Bulk", sex = "Male",
            occupation = "Retired pro hockey player turned gambler",
            hobby = "Mountain climbing", hair = "Red", auto = "Convertible",
            feature1 = "A mermaid tattoo marks his right thumb.", feature2 = "Seafood is his favorite.",
            tSex = "male", tHobby = "mt. climbing", tHair = "red",
            tFeature = "tattoo", tVehicle = "convertible",
        ),
        Suspect(
            name = "Scar Graynolt", sex = "Male",
            occupation = "Guitarist on the folk circuit",
            hobby = "Croquet", hair = "Red", auto = "Limousine with shaded windows",
            feature1 = "Sports a five-carat ring on his pinky.", feature2 = "Keeps a 6'8\" valet called \"The Asp\" and can down his own weight in tacos.",
            tSex = "male", tHobby = "croquet", tHair = "red",
            tFeature = "ring", tVehicle = "limousine",
        ),
        Suspect(
            name = "Nick Brunch", sex = "Male",
            occupation = "Former private investigator",
            hobby = "Mountain climbing", hair = "Black", auto = "\"Black Mamba\" motorcycle",
            feature1 = "Favors rumpled trenchcoats and snap-brim fedoras. Brown-eyed, with a moustache.", feature2 = "Big on Mexican food; his Crimefighter's ring never leaves his finger.",
            tSex = "male", tHobby = "mt. climbing", tHair = "black",
            tFeature = "ring", tVehicle = "motorcycle",
        ),
        Suspect(
            name = "Fast Eddie B.", sex = "Male",
            occupation = "Croquet player of world rank",
            hobby = "Croquet", hair = "Raven-haired or black", auto = "Convertible",
            feature1 = "His calling card: a diamond stickpin left at every crime scene.", feature2 = "A flawlessly dressed jet-setter with a taste for Mexican food.",
            tSex = "male", tHobby = "croquet", tHair = "black",
            tFeature = "jewelry", tVehicle = "convertible",
        ),
        Suspect(
            name = "Ihor Ihorovich", sex = "Male",
            occupation = "Claims the old Czarist throne as his own",
            hobby = "Croquet", hair = "Blond", auto = "Limousine",
            feature1 = "An odd Ukrainian tattoo covers his right shoulder.", feature2 = "Enjoys lobster dinners, cartoons, and everything about large marsupials.",
            tSex = "male", tHobby = "croquet", tHair = "blond",
            tFeature = "tattoo", tVehicle = "limousine",
        ),
        Suspect(
            name = "Katherine \"Boom-Boom\" Drib", sex = "Female",
            occupation = "Races motorcycles competitively",
            hobby = "Mountain climbing", hair = "Brunette or brown", auto = "Honcho-1250 motorcycle",
            feature1 = "An eagle tattoo spreads across her left bicep.", feature2 = "Cooks gourmet seafood; devoted to health and fitness.",
            tSex = "female", tHobby = "mt. climbing", tHair = "brown",
            tFeature = "tattoo", tVehicle = "motorcycle",
        ),
    )

    // Case-flow templates — the English text doubles as the i18n fallback (Strings.ui), so the
    // English build is byte-identical while these become translatable. %s slots are preserved and
    // still filled by the existing call-site .replace("%s", …).
    private fun t(en: String) = com.acme.clara.i18n.Strings.ui(en)
    val FLASH get() = t("*** FLASH ***")
    val TREASURE_STOLEN get() = t("A national treasure has been stolen in %s.")
    val TREASURE_ID get() = t("Reports identify the stolen item as %s.")
    // Whole-sentence per-sex variants: translators can't build gender from swapped-in pronoun
    // words ("%s hideout"), so each sex gets its own complete template.
    val ASSIGNMENT_F get() = t("Follow the thief's trail from %s to her hideout and bring her in!")
    val ASSIGNMENT_M get() = t("Follow the thief's trail from %s to his hideout and bring him in!")
    val DEADLINE get() = t("The thief must be in custody by Sunday at 5 p.m.")
    val WARRANT_ISSUED get() = t("A warrant has been issued for the arrest of %s.")
    val NO_WARRANT get() = t("There is no arrest warrant on file.")
    val ELIMINATES_ALL get() = t("The details you entered rule out every suspect on file.")
    val CAUGHT_UP get() = t("You have finally cornered %s.")
    val NO_WARRANT_ESCAPE get() = t("But with no warrant in hand, no lawful arrest can be made!")
    val GOT_AWAY get() = t("Once again, Clara's gang has slipped away with the loot!")
    val TRAILED_CORRECTLY get() = t("Your pursuit of %s was right on the mark.")
    val FALSE_WARRANT get() = t("Sadly, the warrant you hold names %s.")
    val FALSE_ARREST get() = t("Watch out - a wrongful arrest could land us all in court!")
    val APPREHENDED get() = t("With your help, police in %s have taken %s into custody.")
    val LOOT get() = t("%s was carrying the stolen %s, now on its way home to the thankful people of %s.")
    val THANKS get() = t("Everyone at Interpol appreciates your fine work on this case.")
    val PROMOTION get() = t("Well done, %s - a promotion is yours.")
    val NEW_RANK get() = t("You now hold the rank of %s.")
    val TOO_LONG get() = t("Word just came in: %s escaped because the investigation ran out of time!")
    val CLARA_JAILED get() = t("You've captured the ring-leader herself - Clara San Diego is behind bars for good!")
    val HALL_OF_FAME get() = t("Congratulations - you've earned a place in the Interpol Hall of Fame!")

    // Promotion quiz: almanac fill-in-the-blank (matched case-insensitively). [id] is a stable key
    // for localizing the question + answer independently — see Strings.quiz().
    val promotionQuiz = listOf(
        QuizItem("japan_mainland", "______ Island is the \"mainland\" of Japan. (See Japan, Geography: Topography)", "Honshu"),
        QuizItem("iraq_capital", "The capital of Iraq, on the Tigris River, is ______. (See Iraq, Geography)", "Baghdad"),
        QuizItem("egypt_river", "The ______ River is the longest river in Egypt and in all of Africa.", "Nile"),
        QuizItem("everest", "Mount ______ is the highest mountain in the world, in the Himalayas of Nepal.", "Everest"),
        QuizItem("san_marino", "The smallest and oldest republic in Europe is San ______.", "Marino"),
        QuizItem("norway_capital", "______ is the capital of Norway and its major port.", "Oslo"),
        QuizItem("sydney_opera", "The famous opera house with sail-shaped roofs is found in ______, Australia.", "Sydney"),
        QuizItem("machu_picchu", "Machu Picchu, the lost city of the Incas, is located in ______. (See Peru)", "Peru"),
        QuizItem("comoros_capital", "The Comoros island capital of ______ lies between Madagascar and Africa.", "Moroni"),
        QuizItem("sri_lanka", "______ is the island nation south of India famous for tea and cinnamon.", "Sri Lanka"),
        QuizItem("moscow", "Red Square and St. Basil's cathedral are landmarks of ______.", "Moscow"),
        QuizItem("suez_canal", "The ______ Canal in Egypt connects the Mediterranean and the Red Sea.", "Suez"),
    )
}

data class QuizItem(val id: String, val question: String, val answer: String)
