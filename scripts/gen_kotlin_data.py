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
 "Lady Agatha Wayland":        ("female","tennis","red","ring","race car"),
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
occs=[clean(o).lstrip("$") for o in g["occupations"] if clean(o)]
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
K.append("    "+klist("noInformation", noinfo))
K.append("    "+klist("clueLeadIns", leadins))
K.append("    "+klist("dangerMessages", danger))
K.append("    "+klist("ranks", ranks))
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
K.append("}")
out=os.path.join(BASE,"android/app/src/main/java/com/acme/carmen/data/GameData.kt")
os.makedirs(os.path.dirname(out),exist_ok=True)
open(out,"w").write("\n".join(K)+"\n")
print("wrote",out,"| suspects:",len(suspects),"cities:",len(cities),"venues:",len(venues),"occupations:",len(occs))
