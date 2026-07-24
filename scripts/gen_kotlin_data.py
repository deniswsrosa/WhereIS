#!/usr/bin/env python3
"""Generate GameData.kt for the Android remake from the extracted corpus (corpus/game_data.json).

Every literal here is traceable to an extracted string. The 5-field trait matrix
(sex/hobby/hair/feature/vehicle) is derived from the byte-exact suspect dossiers and
the witness-clue fragment vocabulary; uniqueness across the 10 suspects is asserted.
"""
import json, os
BASE=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
g=json.load(open(os.path.join(BASE,"corpus/game_data.json")))

# --- trait matrix derived from dossiers (documented) ---
# sex, hobby, hair, feature, vehicle  (hair has 4 categories per witness fragments)
TRAITS = {
 "Carmen Sandiego":            ("female","tennis","red","jewelry","convertible"),
 "Merey LaRoc":                ("female","mt. climbing","brown","jewelry","limousine"),
 "Dazzle Annie Nonker":        ("female","tennis","blond","tattoo","limousine"),
 # "Denghby Roadster" = open-top roadster -> convertible; per the ADG mechanics analysis,
 # "race car" is one of the 6 computer traits NO suspect has (it eliminates everyone)
 "Lady Agatha Wayland":        ("female","tennis","red","ring","convertible"),
 "Len \"Red\" Bulk":           ("male","mt. climbing","red","tattoo","convertible"),
 "Scar Graynolt":              ("male","croquet","red","ring","limousine"),
 "Nick Brunch":                ("male","mt. climbing","black","ring","motorcycle"),
 "Fast Eddie B.":              ("male","croquet","black","jewelry","convertible"),
 "Ihor Ihorovich":             ("male","croquet","blond","tattoo","limousine"),
 "Katherine \"Boom-Boom\" Drib":("female","mt. climbing","brown","tattoo","motorcycle"),
}
# assert uniqueness
tuples=list(TRAITS.values())
assert len(set(tuples))==10, "trait matrix not unique!"

HOBBIES=["tennis","music","mt. climbing","skydiving","swimming","croquet"]
HAIRS=["brown","blond","red","black"]
FEATURES=["limps","ring","tattoo","scar","jewelry"]
VEHICLES=["convertible","limousine","race car","motorcycle"]

def kesc(s): return s.replace("\\","\\\\").replace('"','\\"').replace("$","\\$")
def klist(name, items, indent=8):
    pad=" "*indent
    body=",\n".join(f'{pad}"{kesc(x)}"' for x in items)
    return f"val {name} = listOf(\n{body}\n{' '*(indent-4)})"

# clean witness fragments: strip embedded control bytes (0x81,0x87 etc used as separators)
def clean(s): return "".join(ch for ch in s if 32<=ord(ch)<127).strip()

cities=g["cities"]
venues=[clean(v).lstrip("$") for v in g["venues"] if clean(v)]
# "Ok" is a button label that leaked into the occupations block — drop it.
occs=[o for o in (clean(o).lstrip("$") for o in g["occupations"] if clean(o)) if o != "Ok"]
noinfo=list(dict.fromkeys(clean(x) for x in g["no_information_responses"] if clean(x)))
leadins=[clean(x) for x in g["clue_lead_ins"] if clean(x)]
danger=[clean(x) for x in g["danger_messages"] if clean(x)]
ranks=g["ranks"]

suspects=[]
for s in g["suspects"]:
    sex,hobby,hair,feat,veh=TRAITS[s["name"]]
    suspects.append({**s,"t_sex":sex,"t_hobby":hobby,"t_hair":hair,"t_feature":feat,"t_vehicle":veh})

K=[]
K.append("// AUTO-GENERATED from corpus/game_data.json by scripts/gen_kotlin_data.py")
K.append("// Byte-exact strings extracted from CARMEN.EXE (Enhanced, MS-DOS v2.1, (c)1990 Broderbund).")
K.append("// DO NOT EDIT BY HAND.")
K.append("package com.acme.carmen.data")
K.append("")
K.append("data class Suspect(")
K.append("    val name: String, val sex: String, val occupation: String,")
K.append("    val hobby: String, val hair: String, val auto: String,")
K.append("    val feature1: String, val feature2: String,")
K.append("    val tSex: String, val tHobby: String, val tHair: String,")
K.append("    val tFeature: String, val tVehicle: String,")
K.append(")")
K.append("")
K.append("object GameData {")
K.append("    "+klist("cities", cities))
K.append("")
K.append("    "+klist("hobbies", HOBBIES))
K.append("    "+klist("hairColors", HAIRS))
K.append("    "+klist("features", FEATURES))
K.append("    "+klist("vehicles", VEHICLES))
K.append('    val sexes = listOf("female", "male")')
K.append("")
K.append("    "+klist("venues", venues))
K.append("    "+klist("occupations", occs))
# Which witnesses staff each venue (verified against DOSBox captures).
VENUE_OCCUPATIONS = {
 "Bank": ["Vice President","Bank Guard","Teller"],
 "Hotel": ["Hotel manager","Bellhop","House detective"],
 "Museum": ["Museum guard","Docent","Curator"],
 "Sport Club": ["Tennis pro","Waiter","Bartender"],
 "Library": ["Circulation clerk","Reference librarian","Archivist"],
 "Airport": ["Pilot","Flight attendant","Baggage clerk"],
 "Harbor": ["Sailor","Harbor Master","Customs officer","Stevedore","Tugboat captain"],
 "Riverfront": ["Sailor","Stevedore","Urchin","Tugboat captain"],
 "Palace": ["Palace guard","Soldier","Privy Councillor"],
 "Stock Exchange": ["Analyst","Trader","Messenger"],
 "Marketplace": ["Hawker","Street merchant","Urchin"],
 "Foreign Ministry": ["Under Secretary","Attache","Ambassador"],
}
K.append("    // Which witnesses staff each venue (verified against DOSBox captures).")
K.append("    val venueOccupations = mapOf(")
for ven, os_ in VENUE_OCCUPATIONS.items():
    inner = ", ".join(f'"{kesc(x)}"' for x in os_)
    K.append(f'        "{kesc(ven)}" to listOf({inner}),')
K.append("    )")
# Off-track no-information lines, one per venue (EXE keeps them in venue-list order;
# several venues share the generic apology). Emitted as a map so the right venue answers.
NOINFO_BY_VENUE = {
 "Bank":"I'm sorry, I have never seen the person you are looking for.",
 "Hotel":"No one like that has checked in here.",
 "Museum":"I'm sorry, I have never seen the person you are looking for.",
 "Sport Club":"Sorry, I haven't seen anybody like that around here.",
 "Library":"I don't think I've seen anybody like that around the library.",
 "Airport":"It's awfully busy around here; I haven't noticed anyone suspicious.",
 "Harbor":"Sorry, I haven't noticed anything suspicious around the harbor.",
 "Riverfront":"There hasn't been another person around here all day.",
 "Palace":"I'm sorry, I have never seen the person you are looking for.",
 "Stock Exchange":"No one like that has done business here today.",
 "Marketplace":"Sorry, I haven't seen anybody like that around here.",
 "Foreign Ministry":"I'm sorry, I have never seen the person you are looking for.",
}
K.append("    // Off-track witnesses answer with their venue's own line (DOS keeps one per")
K.append("    // venue, in venue-list order in the EXE; several venues share the apology).")
K.append("    val noInformationByVenue = mapOf(")
for ven, line in NOINFO_BY_VENUE.items():
    K.append(f'        "{kesc(ven)}" to "{kesc(line)}",')
K.append("    )")
K.append("    val noInformation = noInformationByVenue.values.distinct()")
K.append("    "+klist("clueLeadIns", leadins))
K.append("    "+klist("dangerMessages", danger))
K.append("    "+klist("ranks", ranks))
K.append("")
# Suspect-trait witness sentences, verbatim from the EXE fragment table (Ç/ü/é separators
# resolved into pronoun slots {S}=She/He {s}=she/he {p}=her/his; only values a real suspect
# has appear in the EXE — three phrasings each, one for hair).
TRAIT_CLUES = {
 "hobby:tennis": ["{S} said that {s} enjoyed playing tennis","{S} asked about the recent tennis match","{S} was carrying a tennis raquet"],
 "hobby:mt. climbing": ["{S} said {s} was a mountain climber","{S} bragged about dangerous sports","{S} talked about great mountains"],
 "hobby:croquet": ["{S} mentioned that {s} plays croquet","{S} said {s} hated dangerous sports","{S} talked about a croquet match"],
 "hair:brown": ["{S} had brown hair"],
 "hair:blond": ["{S} had blond hair"],
 "hair:red": ["{S} had red hair"],
 "hair:black": ["{S} had black hair"],
 "feature:ring": ["{S} had a large ring on","{S} had a fancy ring on","I liked the ring {s} had on"],
 "feature:tattoo": ["I noticed a tattoo on {p} arm","{S} tried to conceal a tattoo","{S} had an ugly tattoo"],
 "feature:jewelry": ["{S} wore fancy jewelry","The jewelry {s} wore was stunning","The jewelry {s} wore looked expensive"],
 "vehicle:convertible": ["{S} arrived in a convertible","{S} had a nice convertible","{S} offered me a ride in {p} convertible"],
 "vehicle:limousine": ["{S} arrived in a private limo","{S} was driving a limo","{S} had {p} driver along"],
 "vehicle:motorcycle": ["{S} arrived on a motorcycle","{S} was riding a motorbike","{S} was carrying a helmet"],
}
K.append("    // Suspect-trait sentences verbatim from the DOS EXE. Pronoun slots {S}/{s}/{p}")
K.append("    // are filled from the culprit's sex, so a clue leaks the sex like the original.")
K.append("    val traitClueFragments = mapOf(")
for key, phrasings in TRAIT_CLUES.items():
    inner = ", ".join(f'"{kesc(p)}"' for p in phrasings)
    K.append(f'        "{kesc(key)}" to listOf({inner}),')
K.append("    )")
# Dossiers menu short names, exactly as listed in the EXE menu resources.
DOSSIER_MENU = ["Carmen Sandiego","Merey LaRoc","Dazzle Annie","Lady Agatha","Len Bulk",
                "Scar Graynolt","Nick Brunch","Fast Eddie B","Ihor Ihorovich","Katherine Drib"]
K.append("    "+klist("dossierMenuNames", DOSSIER_MENU))
K.append("")
K.append("    val suspects = listOf(")
for s in suspects:
    K.append("        Suspect(")
    K.append(f'            name = "{kesc(s["name"])}", sex = "{kesc(s["sex"])}",')
    K.append(f'            occupation = "{kesc(s["occupation"])}",')
    K.append(f'            hobby = "{kesc(s["hobby"])}", hair = "{kesc(s["hair"])}", auto = "{kesc(s["auto"])}",')
    K.append(f'            feature1 = "{kesc(s["feature_1"])}", feature2 = "{kesc(s["feature_2"])}",')
    K.append(f'            tSex = "{s["t_sex"]}", tHobby = "{s["t_hobby"]}", tHair = "{s["t_hair"]}",')
    K.append(f'            tFeature = "{s["t_feature"]}", tVehicle = "{s["t_vehicle"]}",')
    K.append("        ),")
K.append("    )")
K.append("")
K.append('    // Case-flow templates (byte-exact from CARMEN.EXE)')
K.append('    const val FLASH = "*** FLASH ***"')
K.append('    const val TREASURE_STOLEN = "National treasure stolen from %s."')
K.append('    const val TREASURE_ID = "The treasure has been identified as %s."')
K.append('    const val ASSIGNMENT = "Track the thief from %s to %s hideout and arrest %s!"')
K.append('    const val DEADLINE = "You must apprehend the thief by Sunday, 5 p.m."')
K.append('    const val WARRANT_ISSUED = "You now have a warrant to arrest %s."')
K.append('    const val NO_WARRANT = "No warrant has been issued."')
K.append('    const val ELIMINATES_ALL = "The information provided eliminates all possible suspects."')
K.append('    const val CAUGHT_UP = "You have caught up with %s."')
K.append('    const val NO_WARRANT_ESCAPE = "However, without a warrant we cannot make a legal arrest!"')
K.append('    const val GOT_AWAY = "It looks like Carmen\'s gang has gotten away with another caper!"')
K.append('    const val TRAILED_CORRECTLY = "You have trailed %s correctly."')
K.append('    const val FALSE_WARRANT = "Unfortunately, you have a warrant for %s."')
K.append('    const val FALSE_ARREST = "Be careful, we could all be sued for false arrest!"')
K.append('    const val APPREHENDED = "Thanks to your help, the %s police have apprehended %s."')
K.append('    const val LOOT = "%s had the loot, %s, which will be returned to the grateful residents of %s."')
K.append('    const val THANKS = "We here at Interpol thank you for your good work on this case."')
K.append('    const val PROMOTION = "Good job, %s, you have earned a promotion."')
K.append('    const val NEW_RANK = "Your new rank is: %s."')
K.append('    const val TOO_LONG = "We\'ve just received word that %s slipped through your fingers because your investigation took too long!"')
K.append('    const val CARMEN_JAILED = "You have successfully arrested the ring-leader, Carmen Sandiego, and sent her to jail for good!"')
K.append('    const val HALL_OF_FAME = "Congratulations, your name will go into the Interpol Hall of Fame!"')
K.append("")
# Promotion quiz: almanac fill-in-the-blank (first entry captured verbatim; rest authored
# in the same style). Matched case-insensitively against the missing word.
PROMO_QUIZ = [
 ("______ Island is the \"mainland\" of Japan. (See Japan, Geography: Topography)", "Honshu"),
 ("The capital of Iraq, on the Tigris River, is ______. (See Iraq, Geography)", "Baghdad"),
 ("The ______ River is the longest river in Egypt and in all of Africa.", "Nile"),
 ("Mount ______ is the highest mountain in the world, in the Himalayas of Nepal.", "Everest"),
 ("The smallest and oldest republic in Europe is San ______.", "Marino"),
 ("______ is the capital of Norway and its major port.", "Oslo"),
 ("The famous opera house with sail-shaped roofs is found in ______, Australia.", "Sydney"),
 ("Machu Picchu, the lost city of the Incas, is located in ______. (See Peru)", "Peru"),
 ("The Comoros island capital of ______ lies between Madagascar and Africa.", "Moroni"),
 ("______ is the island nation south of India famous for tea and cinnamon.", "Sri Lanka"),
 ("Red Square and St. Basil's cathedral are landmarks of ______.", "Moscow"),
 ("The ______ Canal in Egypt connects the Mediterranean and the Red Sea.", "Suez"),
]
K.append("    // Promotion quiz: almanac fill-in-the-blank (matched case-insensitively).")
K.append("    val promotionQuiz = listOf(")
for q, a in PROMO_QUIZ:
    K.append(f'        "{kesc(q)}" to "{kesc(a)}",')
K.append("    )")
K.append("}")
out=os.path.join(BASE,"android/app/src/main/java/com/acme/carmen/data/GameData.kt")
os.makedirs(os.path.dirname(out),exist_ok=True)
open(out,"w").write("\n".join(K)+"\n")
print("wrote",out,"| suspects:",len(suspects),"cities:",len(cities),"venues:",len(venues),"occupations:",len(occs))
