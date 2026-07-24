// AUTO-GENERATED from corpus/game_data.json by scripts/gen_kotlin_data.py
// Byte-exact strings extracted from CARMEN.EXE (Enhanced, MS-DOS v2.1, (c)1990 Broderbund).
// DO NOT EDIT BY HAND.
package com.acme.carmen.data

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
        "Peking",
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
        "All I know is that something suspicious is going on in town.",
        "Rumor has it that the gang is in town somewhere.",
        "The word is out: You're getting too close, gumshoe...",
        "The only thing I can tell you is to watch your step!",
        "Yes",
        "No",
        ":"
    )
    val ranks = listOf(
        "Rookie",
        "Sleuth",
        "Private Eye",
        "Investigator",
        "Ace Detective"
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
        "Carmen Sandiego",
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
            name = "Carmen Sandiego", sex = "Female",
            occupation = "Former spy for the Intelligence Service of Monaco",
            hobby = "Tennis", hair = "Reddish-brown", auto = "1939 Packard convertible",
            feature1 = "Never appears in public without her ruby necklace.", feature2 = "Great fondness for tacos.",
            tSex = "female", tHobby = "tennis", tHair = "red",
            tFeature = "jewelry", tVehicle = "convertible",
        ),
        Suspect(
            name = "Merey LaRoc", sex = "Female",
            occupation = "Freelance aerobic dancer",
            hobby = "Mountain climbing", hair = "Brown", auto = "Fancy limousine",
            feature1 = "Has an absolute mania for fancy jewelry.", feature2 = "Loves spicy foods.",
            tSex = "female", tHobby = "mt. climbing", tHair = "brown",
            tFeature = "jewelry", tVehicle = "limousine",
        ),
        Suspect(
            name = "Dazzle Annie Nonker", sex = "Female",
            occupation = "Yogurt bar owner",
            hobby = "Tennis", hair = "Blond", auto = "Bugatti Limousine",
            feature1 = "Reported to have a tattoo.", feature2 = "Has an incredible craving for shellfish.",
            tSex = "female", tHobby = "tennis", tHair = "blond",
            tFeature = "tattoo", tVehicle = "limousine",
        ),
        Suspect(
            name = "Lady Agatha Wayland", sex = "Female",
            occupation = "Reader of upper-class English mystery stories",
            hobby = "Tennis", hair = "Red", auto = "Denghby Roadster",
            feature1 = "Has a diamond ring the size of a grapefruit.", feature2 = "Speeds through the countryside looking for great Mexican restaurants.",
            tSex = "female", tHobby = "tennis", tHair = "red",
            tFeature = "ring", tVehicle = "convertible",
        ),
        Suspect(
            name = "Len \"Red\" Bulk", sex = "Male",
            occupation = "Ex-professional hockey player and gambler",
            hobby = "Mountain climbing", hair = "Red", auto = "Convertible",
            feature1 = "Tattoo of mermaid on his right thumb.", feature2 = "Loves seafood.",
            tSex = "male", tHobby = "mt. climbing", tHair = "red",
            tFeature = "tattoo", tVehicle = "convertible",
        ),
        Suspect(
            name = "Scar Graynolt", sex = "Male",
            occupation = "Folk guitarist",
            hobby = "Croquet", hair = "Red", auto = "Limousine with shaded windows",
            feature1 = "Wears a five-carat pinky ring.", feature2 = "Has a 6'8\" man servant named \"The Asp\"; can eat his own weight in tacos.",
            tSex = "male", tHobby = "croquet", tHair = "red",
            tFeature = "ring", tVehicle = "limousine",
        ),
        Suspect(
            name = "Nick Brunch", sex = "Male",
            occupation = "Ex-private eye",
            hobby = "Mountain climbing", hair = "Black", auto = "\"Black Mamba\" motorcycle",
            feature1 = "Prefers soiled trenchcoats and snap-brimmed fedoras.  Has brown eyes and a moustache.", feature2 = "Loves Mexican food; always wears Crimefighter's ring.",
            tSex = "male", tHobby = "mt. climbing", tHair = "black",
            tFeature = "ring", tVehicle = "motorcycle",
        ),
        Suspect(
            name = "Fast Eddie B.", sex = "Male",
            occupation = "World class croquet player",
            hobby = "Croquet", hair = "Raven-haired or black", auto = "Convertible",
            feature1 = "Always leaves a diamond stickpin at the scene of his crimes.", feature2 = "Fast Eddie is an impeccably dressed jet-setter and likes Mexican food.",
            tSex = "male", tHobby = "croquet", tHair = "black",
            tFeature = "jewelry", tVehicle = "convertible",
        ),
        Suspect(
            name = "Ihor Ihorovich", sex = "Male",
            occupation = "Pretender to the Czarist throne",
            hobby = "Croquet", hair = "Blond", auto = "Limousine",
            feature1 = "Strange Ukranian tattoo on right shoulder.", feature2 = "Loves eating lobsters, watching cartoons and is fascinated by large marsupials.",
            tSex = "male", tHobby = "croquet", tHair = "blond",
            tFeature = "tattoo", tVehicle = "limousine",
        ),
        Suspect(
            name = "Katherine \"Boom-Boom\" Drib", sex = "Female",
            occupation = "Motorcycle racer",
            hobby = "Mountain climbing", hair = "Brunette or brown", auto = "Honcho-1250 motorcycle",
            feature1 = "Has a tattoo of an eagle on her left bicep.", feature2 = "Gourmet seafood cook; fascinated with health and fitness.",
            tSex = "female", tHobby = "mt. climbing", tHair = "brown",
            tFeature = "tattoo", tVehicle = "motorcycle",
        ),
    )

    // Case-flow templates (byte-exact from CARMEN.EXE)
    const val FLASH = "*** FLASH ***"
    const val TREASURE_STOLEN = "National treasure stolen from %s."
    const val TREASURE_ID = "The treasure has been identified as %s."
    const val ASSIGNMENT = "Track the thief from %s to %s hideout and arrest %s!"
    const val DEADLINE = "You must apprehend the thief by Sunday, 5 p.m."
    const val WARRANT_ISSUED = "You now have a warrant to arrest %s."
    const val NO_WARRANT = "No warrant has been issued."
    const val ELIMINATES_ALL = "The information provided eliminates all possible suspects."
    const val CAUGHT_UP = "You have caught up with %s."
    const val NO_WARRANT_ESCAPE = "However, without a warrant we cannot make a legal arrest!"
    const val GOT_AWAY = "It looks like Carmen's gang has gotten away with another caper!"
    const val TRAILED_CORRECTLY = "You have trailed %s correctly."
    const val FALSE_WARRANT = "Unfortunately, you have a warrant for %s."
    const val FALSE_ARREST = "Be careful, we could all be sued for false arrest!"
    const val APPREHENDED = "Thanks to your help, the %s police have apprehended %s."
    const val LOOT = "%s had the loot, %s, which will be returned to the grateful residents of %s."
    const val THANKS = "We here at Interpol thank you for your good work on this case."
    const val PROMOTION = "Good job, %s, you have earned a promotion."
    const val NEW_RANK = "Your new rank is: %s."
    const val TOO_LONG = "We've just received word that %s slipped through your fingers because your investigation took too long!"
    const val CARMEN_JAILED = "You have successfully arrested the ring-leader, Carmen Sandiego, and sent her to jail for good!"
    const val HALL_OF_FAME = "Congratulations, your name will go into the Interpol Hall of Fame!"

    // Promotion quiz: almanac fill-in-the-blank (matched case-insensitively).
    val promotionQuiz = listOf(
        "______ Island is the \"mainland\" of Japan. (See Japan, Geography: Topography)" to "Honshu",
        "The capital of Iraq, on the Tigris River, is ______. (See Iraq, Geography)" to "Baghdad",
        "The ______ River is the longest river in Egypt and in all of Africa." to "Nile",
        "Mount ______ is the highest mountain in the world, in the Himalayas of Nepal." to "Everest",
        "The smallest and oldest republic in Europe is San ______." to "Marino",
        "______ is the capital of Norway and its major port." to "Oslo",
        "The famous opera house with sail-shaped roofs is found in ______, Australia." to "Sydney",
        "Machu Picchu, the lost city of the Incas, is located in ______. (See Peru)" to "Peru",
        "The Comoros island capital of ______ lies between Madagascar and Africa." to "Moroni",
        "______ is the island nation south of India famous for tea and cinnamon." to "Sri Lanka",
        "Red Square and St. Basil's cathedral are landmarks of ______." to "Moscow",
        "The ______ Canal in Egypt connects the Mediterranean and the Red Sea." to "Suez",
    )
}
