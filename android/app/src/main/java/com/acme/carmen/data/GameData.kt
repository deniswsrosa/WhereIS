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

    /** Which witnesses staff each venue — matches the original game's pairings
     *  (verified against DOSBox captures: Harbor→Customs officer/Sailor, Museum→Docent,
     *  Library→Circulation clerk/Reference librarian/Archivist, Palace→Soldier, etc.) */
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
    val noInformation = listOf(
        "I'm sorry, I have never seen the person you are looking for.",
        "No one like that has checked in here.",
        "Sorry, I haven't seen anybody like that around here.",
        "I don't think I've seen anybody like that around the library.",
        "It's awfully busy around here; I haven't noticed anyone suspicious.",
        "Sorry, I haven't noticed anything suspicious around the harbor.",
        "There hasn't been another person around here all day.",
        "No one like that has done business here today."
    )
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
            tFeature = "ring", tVehicle = "race car",
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
}
