package com.acme.clara.data

/**
 * Per-city geography used for destination clues and the info panel.
 *
 * `real = true`  -> description transcribed verbatim from an authentic in-game screen
 *                   (CARMEN##.BMP captured playthrough shipped in the archive item).
 * `real = false` -> remake-authored. Either framed to the game's 1990 period (e.g.
 *                   "Soviet Union", "Peking") or, where the original stated facts that
 *                   are now false (populations, defunct countries such as Yugoslavia),
 *                   updated to present-day data. NOT byte-exact original text.
 *
 * The original Enhanced build does not store these passages as extractable strings;
 * they are rendered into the city artwork. See corpus/carmen_corpus.md, "Negative findings".
 */
data class CityInfo(
    val name: String,
    val region: String,
    val landmark: String,
    val description: String,
    val real: Boolean,
    val drawable: String? = null, // resource name if we have the authentic photo
    // Expansion clue data (empty/null for the original 30, whose clues come from region+landmark
    // templates). `clues` are hand-authored forward-looking witness leads — one per angle,
    // exactly the pattern the original used. `greeting`/`flag`/`currency` are structured
    // attributes destinationClue() can also template a clue from (see ClaraViewModel).
    val clues: List<String> = emptyList(),
    val greeting: String? = null, // how the local hello sounds, e.g. "bohn-ZHOOR"
    val flag: String? = null,     // short flag description, e.g. "a single yellow star on red"
    val currency: String? = null, // local money, e.g. "rubles"
)

object CityMeta {
    val all: Map<String, CityInfo> = listOf(
        CityInfo("Athens", "Europe", "the Parthenon",
            "In ancient times, Athens was a powerful city-state that warred with its neighbor Sparta and made lasting contributions to philosophy, science, drama and art.", true, "city_athens", greeting = "In Greek, hello is “Yassas” (YAH-sas)."),
        CityInfo("Baghdad", "the Middle East", "the Tigris River",
            "Baghdad, with a population of about 8 million, is Iraq's capital and largest city.", false, "city_baghdad", greeting = "In Arabic, hello is “Marhaba” (MAR-hah-bah)."),
        CityInfo("Bamako", "Africa", "Timbuktu",
            "Bamako, with a population of around 3 million, is by far the largest city in Mali, an arid country located in West Africa and extending into the Sahara Desert.", false, "city_bamako", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR)."),
        CityInfo("Bangkok", "Asia", "temples called wats",
            "Bangkok, the capital of Thailand, is a bustling city laced with canals and dotted with temples called wats.", true, "city_bangkok", greeting = "In Thai, hello is “Sawasdee” (sah-wah-DEE)."),
        CityInfo("Budapest", "Europe", "the Danube",
            "Hungary, with an area slightly smaller than Indiana, is bordered by Slovakia, Austria, Slovenia, Croatia, Serbia, Romania and Ukraine.", false, "city_budapest", greeting = "In Hungarian, a friendly hi is “Szia” (SEE-yah)."),
        CityInfo("Buenos Aires", "South America", "Patagonia",
            "Argentina is South America's second-largest nation, after Brazil. Its terrain ranges from tropical forests in the north to cold and barren Tierra del Fuego in the south.", true, "city_buenos_aires", greeting = "In Spanish, hello is “Hola” (OH-lah)."),
        CityInfo("Cairo", "Africa", "the Pyramids",
            "Cairo, located at the mouth of the Nile River, is the largest city in Africa.", true, "city_cairo", greeting = "In Arabic, hello is “Salaam” (sah-LAAM)."),
        CityInfo("Colombo", "Asia", "the island once called Ceylon",
            "Sri Lanka has a hot, humid climate. The principal crops grown on this island nation are tea, coconuts and rice.", true, "city_colombo", greeting = "In Sinhala, hello is “Ayubowan” (ah-yoo-BOH-wan)."),
        CityInfo("Istanbul", "Europe", "the Bosporus",
            "Turkey, which has hot, dry summers and cold winters, is ringed by high mountains on all but the western border.", true, "city_istanbul", greeting = "In Turkish, hello is “Merhaba” (mehr-hah-BAH)."),
        CityInfo("Kathmandu", "Asia", "the Himalayas",
            "Nepal was closed to the outside world for centuries, but can now be reached by plane and road from India, Pakistan and Tibet.", true, "city_kathmandu", greeting = "In Nepali, a greeting is “Namaste” (nah-mah-STAY)."),
        CityInfo("Kigali", "Africa", "the gorilla volcanoes",
            "Kigali is the capital of Rwanda, a country of lush jungle terrain which is the home of the endangered mountain gorilla.", true, "city_kigali", greeting = "In Kinyarwanda, hello is “Muraho” (moo-RAH-ho)."),
        CityInfo("Lima", "South America", "Machu Picchu",
            "Peru, once the center of the mighty Incan Empire, is a rugged land dominated by the Andes Mountains. Forests and jungles cover half its land area.", true, "city_lima", greeting = "In Spanish, hello is “Hola” (OH-lah)."),
        CityInfo("London", "Europe", "Big Ben",
            "London is the capital of the United Kingdom, which consists of England, Scotland, Wales and Northern Ireland.", true, "city_london", greeting = "In Britain, a cheerful hello is “Hiya” (HY-uh)."),
        CityInfo("Mexico City", "North America", "the Zocalo",
            "Mexico is about three times the size of Texas, and has terrain ranging from rugged mountains to harsh deserts and tropical lowlands.", true, "city_mexico_city", greeting = "In Spanish, hello is “Hola” (OH-lah)."),
        CityInfo("Montreal", "North America", "the St. Lawrence River",
            "Montreal is the second largest city in Canada. A famous landmark is the church of Notre Dame de Bonsecours.", true, "city_montreal", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR)."),
        CityInfo("Moroni", "Africa", "the islands north of Madagascar",
            "The chief industry of the Comoros is perfume, made from plants grown on the islands.", true, "city_moroni", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR)."),
        CityInfo("Moscow", "Europe", "Red Square",
            "The Soviet Union is the largest country in the world. It occupies one-sixth of the earth's land area.", true, "city_moscow", greeting = "In Russian, a friendly hi is “Privet” (pree-VYET)."),
        CityInfo("New Delhi", "Asia", "the Red Fort",
            "New Delhi, with a metropolitan population of more than 30 million, is the capital of India, a nation of over 1.4 billion people.", false, "city_new_delhi", greeting = "In Hindi, a greeting is “Namaste” (nah-mah-STAY)."),
        CityInfo("New York", "North America", "the Statue of Liberty",
            "The headquarters of the United Nations, located in New York, adds to the multi-cultural nature of the city.", true, "city_new_york", greeting = "In the US, a casual hi is “Hey there” (hay thair)."),
        CityInfo("Oslo", "Europe", "the Oslofjord",
            "Oslo, the capital and largest city in Norway, is a major industrial and maritime center as well as the seat of government.", true, "city_oslo", greeting = "In Norwegian, a friendly hi is “Hei” (hay)."),
        CityInfo("Paris", "Europe", "the Eiffel Tower",
            "Paris, the capital of France, is a center of art and culture, and home of the world famous Eiffel Tower.", true, "city_paris", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR)."),
        CityInfo("Peking", "Asia", "the Great Wall",
            "Peking is the capital of the People's Republic of China. One of this nation's most famous landmarks is the Great Wall.", true, "city_peking", greeting = "In Mandarin, hello is “Nǐ hǎo” (nee how)."),
        CityInfo("Port Moresby", "Oceania", "the highlands just north of Australia",
            "Port Moresby is the capital of Papua New Guinea, a land of jungles and volcanic mountains.", true, "city_port_moresby", greeting = "In Tok Pisin, hello is “Halo” (HAH-loh)."),
        CityInfo("Reykjavik", "Europe", "geysers and volcanoes",
            "Iceland's governmental assembly, the Althing, is the world's oldest surviving parliament.", true, "city_reykjavik", greeting = "In Icelandic, hello is “Halló” (HAH-loh)."),
        CityInfo("Rio de Janeiro", "South America", "Christ the Redeemer",
            "Brazil's capital is Brasilia, an entirely new city recently built in the interior of the country.", true, "city_rio_de_janeiro", greeting = "In Portuguese, a casual hi is “Oi” (oy)."),
        CityInfo("Rome", "Europe", "the Colosseum",
            "During the Renaissance, Italy was divided into numerous city-states which were vital centers of art, science and learning.", true, "city_rome", greeting = "In Italian, a casual hi is “Ciao” (chow)."),
        CityInfo("San Marino", "Europe", "castle towers surrounded by Italy",
            "Postage stamps and tourism are San Marino's largest industries. This tiny country's full name is Most Serene Republic of San Marino.", true, "city_san_marino", greeting = "In Italian, people greet you with “Buongiorno” (bwohn-JOR-noh) — it means “good day.”"),
        CityInfo("Singapore", "Asia", "the tip of the Malay Peninsula",
            "The nation of Singapore, located near the equator in Southeast Asia, consists of one large island plus 40 smaller islands.", true, "city_singapore", greeting = "In Malay, “Apa khabar?” (AH-pah KAH-bar) means “how are you?”."),
        CityInfo("Sydney", "Oceania", "the Opera House",
            "An island continent, Australia is nearly as large as the United States but has only about one-twelfth the population.", false, "city_sydney", greeting = "In Australia, a classic hello is “G’day” (guh-DAY)."),
        CityInfo("Tokyo", "Asia", "Mount Fuji",
            "Tokyo, with a population of about 14 million people, is the capital and largest city in Japan.", false, "city_tokyo", greeting = "In Japanese, hello is “Konnichiwa” (kohn-nee-chee-WAH)."),
    ).associateBy { it.name }

    fun of(name: String): CityInfo = all[name] ?: Expansion.byName[name] ?: Expansion2.byName[name]
        ?: CityInfo(name, "the world", "the city",
            "$name is one of the great cities of the world.", false)
}
