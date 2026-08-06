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
            "In ancient times, Athens was a powerful city-state that warred with its neighbor Sparta and made lasting contributions to philosophy, science, drama and art.", true, "city_athens", greeting = "In Greek, hello is “Yassas” (YAH-sas).", flag = "blue and white stripes with a white cross in the corner", currency = "the euro"),
        CityInfo("Baghdad", "the Middle East", "the Tigris River",
            "Baghdad, with a population of about 8 million, is Iraq's capital and largest city.", false, "city_baghdad", greeting = "In Arabic, hello is “Marhaba” (MAR-hah-bah).", flag = "red, white and black bands with green Arabic script", currency = "the Iraqi dinar"),
        CityInfo("Bamako", "Africa", "Timbuktu",
            "Bamako, with a population of around 3 million, is by far the largest city in Mali, an arid country located in West Africa and extending into the Sahara Desert.", false, "city_bamako", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR).", flag = "three bands of green, yellow and red", currency = "the CFA franc"),
        CityInfo("Bangkok", "Asia", "temples called wats",
            "Bangkok, the capital of Thailand, is a bustling city laced with canals and dotted with temples called wats.", true, "city_bangkok", greeting = "In Thai, hello is “Sawasdee” (sah-wah-DEE).", flag = "red, white and blue bands, the blue one twice as thick", currency = "the baht"),
        CityInfo("Budapest", "Europe", "the Danube",
            "Hungary, with an area slightly smaller than Indiana, is bordered by Slovakia, Austria, Slovenia, Croatia, Serbia, Romania and Ukraine.", false, "city_budapest", greeting = "In Hungarian, a friendly hi is “Szia” (SEE-yah).", flag = "three bands of red, white and green", currency = "the forint"),
        CityInfo("Buenos Aires", "South America", "Patagonia",
            "Argentina is South America's second-largest nation, after Brazil. Its terrain ranges from tropical forests in the north to cold and barren Tierra del Fuego in the south.", true, "city_buenos_aires", greeting = "In Spanish, hello is “Hola” (OH-lah).", flag = "light blue and white bands with a golden sun", currency = "the Argentine peso"),
        CityInfo("Cairo", "Africa", "the Pyramids",
            "Cairo, located at the mouth of the Nile River, is the largest city in Africa.", true, "city_cairo", greeting = "In Arabic, hello is “Salaam” (sah-LAAM).", flag = "red, white and black bands with a golden eagle", currency = "the Egyptian pound"),
        CityInfo("Colombo", "Asia", "the island once called Ceylon",
            "Sri Lanka has a hot, humid climate. The principal crops grown on this island nation are tea, coconuts and rice.", true, "city_colombo", greeting = "In Sinhala, hello is “Ayubowan” (ah-yoo-BOH-wan).", flag = "a golden lion holding a sword beside green and orange stripes", currency = "the Sri Lankan rupee"),
        CityInfo("Istanbul", "Europe", "the Bosporus",
            "Turkey, which has hot, dry summers and cold winters, is ringed by high mountains on all but the western border.", true, "city_istanbul", greeting = "In Turkish, hello is “Merhaba” (mehr-hah-BAH).", flag = "red with a white crescent moon and star", currency = "the Turkish lira"),
        CityInfo("Kathmandu", "Asia", "the Himalayas",
            "Nepal was closed to the outside world for centuries, but can now be reached by plane and road from India, Pakistan and Tibet.", true, "city_kathmandu", greeting = "In Nepali, a greeting is “Namaste” (nah-mah-STAY).", flag = "a crimson double-pennant shape with a white moon and sun", currency = "the Nepalese rupee"),
        CityInfo("Kigali", "Africa", "the gorilla volcanoes",
            "Kigali is the capital of Rwanda, a country of lush jungle terrain which is the home of the endangered mountain gorilla.", true, "city_kigali", greeting = "In Kinyarwanda, hello is “Muraho” (moo-RAH-ho).", flag = "blue, yellow and green bands with a golden sun", currency = "the Rwandan franc"),
        CityInfo("Lima", "South America", "Machu Picchu",
            "Peru, once the center of the mighty Incan Empire, is a rugged land dominated by the Andes Mountains. Forests and jungles cover half its land area.", true, "city_lima", greeting = "In Spanish, hello is “Hola” (OH-lah).", flag = "red, white and red vertical bands", currency = "the sol"),
        CityInfo("London", "Europe", "Big Ben",
            "London is the capital of the United Kingdom, which consists of England, Scotland, Wales and Northern Ireland.", true, "city_london", greeting = "In Britain, a cheerful hello is “Hiya” (HY-uh).", flag = "the Union Jack — layered red and white crosses on blue", currency = "the pound sterling"),
        CityInfo("Mexico City", "North America", "the Zocalo",
            "Mexico is about three times the size of Texas, and has terrain ranging from rugged mountains to harsh deserts and tropical lowlands.", true, "city_mexico_city", greeting = "In Spanish, hello is “Hola” (OH-lah).", flag = "green, white and red bands with an eagle on a cactus", currency = "the peso"),
        CityInfo("Montreal", "North America", "the St. Lawrence River",
            "Montreal is the second largest city in Canada. A famous landmark is the church of Notre Dame de Bonsecours.", true, "city_montreal", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR).", flag = "a red maple leaf between two red bands", currency = "the Canadian dollar"),
        CityInfo("Moroni", "Africa", "the islands north of Madagascar",
            "The chief industry of the Comoros is perfume, made from plants grown on the islands.", true, "city_moroni", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR).", flag = "yellow, white, red and blue bands with a green triangle holding a crescent and four stars", currency = "the Comorian franc"),
        CityInfo("Moscow", "Europe", "Red Square",
            "Russia is the largest country in the world. It occupies about one-eighth of the earth's land area.", false, "city_moscow", greeting = "In Russian, a friendly hi is “Privet” (pree-VYET).", flag = "three bands of white, blue and red", currency = "the ruble"),
        CityInfo("New Delhi", "Asia", "the Red Fort",
            "New Delhi, with a metropolitan population of more than 30 million, is the capital of India, a nation of over 1.4 billion people.", false, "city_new_delhi", greeting = "In Hindi, a greeting is “Namaste” (nah-mah-STAY).", flag = "saffron, white and green bands with a navy spoked wheel", currency = "the Indian rupee"),
        CityInfo("New York", "North America", "the Statue of Liberty",
            "The headquarters of the United Nations, located in New York, adds to the multi-cultural nature of the city.", true, "city_new_york", greeting = "In the US, a casual hi is “Hey there” (hay thair).", flag = "fifty white stars on blue with thirteen red-and-white stripes", currency = "the US dollar"),
        CityInfo("Oslo", "Europe", "the Oslofjord",
            "Oslo, the capital and largest city in Norway, is a major industrial and maritime center as well as the seat of government.", true, "city_oslo", greeting = "In Norwegian, a friendly hi is “Hei” (hay).", flag = "red with a blue-and-white cross", currency = "the Norwegian krone"),
        CityInfo("Paris", "Europe", "the Eiffel Tower",
            "Paris, the capital of France, is a center of art and culture, and home of the world famous Eiffel Tower.", true, "city_paris", greeting = "In French, hello is “Bonjour” (bohn-ZHOOR).", flag = "three bands of blue, white and red", currency = "the euro"),
        CityInfo("Peking", "Asia", "the Great Wall",
            "Peking is the capital of the People's Republic of China. One of this nation's most famous landmarks is the Great Wall.", true, "city_peking", greeting = "In Mandarin, hello is “Nǐ hǎo” (nee how).", flag = "red with one large yellow star and four smaller stars", currency = "the yuan"),
        CityInfo("Port Moresby", "Oceania", "the highlands just north of Australia",
            "Port Moresby is the capital of Papua New Guinea, a land of jungles and volcanic mountains.", true, "city_port_moresby", greeting = "In Tok Pisin, hello is “Halo” (HAH-loh).", flag = "a diagonal split — a bird of paradise on red, the Southern Cross on black", currency = "the kina"),
        CityInfo("Reykjavik", "Europe", "geysers and volcanoes",
            "Iceland's governmental assembly, the Althing, is the world's oldest surviving parliament.", true, "city_reykjavik", greeting = "In Icelandic, hello is “Halló” (HAH-loh).", flag = "blue with a red-and-white cross", currency = "the Icelandic króna"),
        CityInfo("Rio de Janeiro", "South America", "Christ the Redeemer",
            "Brazil's capital is Brasilia, an entirely new city recently built in the interior of the country.", true, "city_rio_de_janeiro", greeting = "In Portuguese, a casual hi is “Oi” (oy).", flag = "green with a yellow diamond and a blue globe crossed by a starry band", currency = "the real"),
        CityInfo("Rome", "Europe", "the Colosseum",
            "During the Renaissance, Italy was divided into numerous city-states which were vital centers of art, science and learning.", true, "city_rome", greeting = "In Italian, a casual hi is “Ciao” (chow).", flag = "three bands of green, white and red", currency = "the euro"),
        CityInfo("San Marino", "Europe", "castle towers surrounded by Italy",
            "Postage stamps and tourism are San Marino's largest industries. This tiny country's full name is Most Serene Republic of San Marino.", true, "city_san_marino", greeting = "In Italian, people greet you with “Buongiorno” (bwohn-JOR-noh) — it means “good day.”", flag = "white above light blue with a shield of three towers", currency = "the euro"),
        CityInfo("Singapore", "Asia", "the tip of the Malay Peninsula",
            "The nation of Singapore, located near the equator in Southeast Asia, consists of one large island plus 40 smaller islands.", true, "city_singapore", greeting = "In Malay, “Apa khabar?” (AH-pah KAH-bar) means “how are you?”.", flag = "red above white with a white crescent moon and five stars", currency = "the Singapore dollar"),
        CityInfo("Sydney", "Oceania", "the Opera House",
            "An island continent, Australia is nearly as large as the United States but has only about one-twelfth the population.", false, "city_sydney", greeting = "In Australia, a classic hello is “G’day” (guh-DAY).", flag = "blue with a Union Jack and the white stars of the Southern Cross", currency = "the Australian dollar"),
        CityInfo("Tokyo", "Asia", "Mount Fuji",
            "Tokyo, with a population of about 14 million people, is the capital and largest city in Japan.", false, "city_tokyo", greeting = "In Japanese, hello is “Konnichiwa” (kohn-nee-chee-WAH).", flag = "white with a large red circle in the center", currency = "the yen"),
    ).associateBy { it.name }

    fun of(name: String): CityInfo = localize(all[name] ?: Expansion.byName[name] ?: Expansion2.byName[name]
        ?: CityInfo(name, "the world", "the city",
            "$name is one of the great cities of the world.", false))

    /** Overlay the active-language translations onto a city's DISPLAY text (keys `city.<name>.<field>`).
     *  English is a no-op — untranslated keys fall back to the Kotlin value, so the build never breaks
     *  and partial translations degrade gracefully. Name/region/currency stay canonical (region is a
     *  logic switch-key; currency names are language-neutral and templated by the clue). */
    private fun localize(c: CityInfo): CityInfo {
        if (com.acme.clara.i18n.Strings.language == "en") return c
        val s = com.acme.clara.i18n.Strings
        val n = c.name
        return c.copy(
            description = s.opt("city.$n.description") ?: c.description,
            landmark = s.opt("city.$n.landmark") ?: c.landmark,
            greeting = c.greeting?.let { s.opt("city.$n.greeting") ?: it },
            flag = c.flag?.let { s.opt("city.$n.flag") ?: it },
            clues = if (c.clues.isEmpty()) c.clues
                    else c.clues.mapIndexed { i, cl -> s.opt("city.$n.clue.$i") ?: cl },
        )
    }
}
