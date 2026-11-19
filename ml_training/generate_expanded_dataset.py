"""
SafeSphere - Person 2 (ML)
Generates an augmented dataset of balanced Hinglish + English text samples
(Adult/Sexual = 1, Normal/Everyday = 0) to provide a solid baseline for training.
"""
import csv
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATASET_PATH = HERE / "dataset" / "dataset.csv"

# Label 1: Adult / Sexual / Solicitation / Explicit content
ADULT_SAMPLES = [
    # English explicit & solicitation
    "send me your nudes",
    "wanna have sex tonight?",
    "show me your body on video call",
    "you look so hot, send a pic without clothes",
    "let's sext, i'm so horny rn",
    "i want to see you naked",
    "come over, parents are out, we can do adult stuff",
    "wanna hook up tonight just us",
    "i'll pay you for nude photos",
    "send me a spicy pic, you know what i mean",
    "tell me what you'd do if we were alone in bed",
    "let me see more skin on cam",
    "watch this xxx video link",
    "free adult videos here, click now",
    "i can't stop thinking about your body",
    "strip for me on webcam",
    "send dirty pictures right now",
    "let's do phone sex tonight",
    "send pictures in your bra and underwear",
    "can you touch yourself on camera for me?",
    "meet me in the hotel room, nobody will know",
    "take off your clothes and turn on video",
    "do you want to see my dick pic?",
    "send nude selfie or i will block you",
    "let's trade private photos, don't be shy",
    "i want to touch you all over",
    "are you a virgin? let's do it",
    "send boobs pic please babe",
    "looking for casual hookups near you tonight",
    "hot adult dating with single girls click here",

    # Hinglish explicit & solicitation
    "nudes bhej na, kisi ko nahi bataunga",
    "tu mere saath sone aa jaa, akele ghar pe hoon",
    "kapde utaar ke video call kar",
    "raat ko hotel chalein? sirf hum dono",
    "tere saath kiss karne ka mann hai aur bhi bahut kuch",
    "bhej na apni private pics yaar",
    "porn dekhna hai? link bhej raha hoon",
    "tu bahut sexy lag rahi hai kuch aur dikha",
    "apni photo bhej bina kapdon ke",
    "chal sex chat karte hain",
    "ek baar milte hain chupke se, kisi ko mat batana, maza aayega",
    "bed pe aa jaa baby aaj raat kuch alag karte hain",
    "sexy pics bhej, main bhi bhejta hoon",
    "ghar pe koi nahi hai, aa ja, adult movie dekhte hain",
    "tere lips pe kiss karna hai aur uske baad bedroom",
    "video call pe strip kar na baby",
    "apni cleavage dikha ek baar",
    "kisi ko pata nahi chalega, bas ek private pic bhej de",
    "aaj raat mere kamre me so jaana chupke se",
    "tera figure bohot mast hai, kapde utaar ke dikha",
    "tu bohot garam lag rahi hai aaj",
    "chupke se milne aaja, sex karenge",
    "apna underwear dikha cam pe",
    "bina kapde ke photo kheench ke bhej na",
    "tu mere saath room chalega kya?",
    "adult link hai dekh le mast hai",
    "chal aaj dirty baatein karte hain raat bhar",
    "ek bar touch karne de na please",
    "tere saath romance karna hai bedroom me",
    "aaj ghar pe koi nahi hai, mauka hai aa ja",
    "apni hot wali photo send kar jaldi",
    "tu aur main ek bed pe, socho kaisa hoga",
    "video call utha aur kapde nikal",
    "tere jism ko choona chahta hoon",
    "apni sabse private photo mujhe dikha",
    "chal na hookup karte hain kisi ko nahi bolenge",
    "room book kar liya hai, 10 baje pohoch jaana",
    "dekhna hai toh video call kar bina sharam ke",
    "raat ko call karna jab sab so jayein, dirty talk karenge",
    "bhai porn website ka latest link bhej"
]

# Label 0: Normal / Everyday chats (school, family, friends, slang, tech, alerts)
NORMAL_SAMPLES = [
    # Hinglish everyday & school
    "kal school ke baad cricket khelne chalein?",
    "homework complete hua kya? maths wala",
    "happy birthday yaar! party kab de raha hai",
    "mom is making biryani tonight come over for dinner",
    "bhai notes bhej dena physics ke",
    "it's so hot outside today, i'm melting",
    "exam kal hai, mujhe bahut tension ho rahi hai",
    "did you watch the match last night? what a finish",
    "chal canteen chalte hain, bhookh lagi hai",
    "the teacher said project submission is on friday",
    "tu kahan hai? main gate pe wait kar raha hoon",
    "love you mom, will call after class",
    "meri bike ka petrol khatam ho gaya, push karna padega",
    "new phone kab le raha hai? iphone ya samsung?",
    "your order has been shipped and will arrive tomorrow",
    "OTP for your transaction is 483920, do not share",
    "aaj ka assignment bohot tough tha yaar",
    "let's study together at the library on sunday",
    "bro you're so dumb lol, that was a hilarious prank",
    "movie tickets book kar di hain, 6 baje milte hain",
    "bhai project ka code GitHub pe push kiya kya?",
    "sir ne kal viva lene ko bola hai",
    "train 15 minute late hai platform 3 pe aayegi",
    "paani ki bottle bhool gaya canteen me",
    "ghar pohoch ke call karna",
    "kal test me cheating mat karna pakde jaoge",
    "gym chalte hain aaj sham ko 7 baje",
    "bhai 50 rupaye gpay kar de auto wale ko dene hain",
    "diwali vacation kab se shuru ho rahi hai?",
    "mera phone switch off hone wala hai 2 percent battery hai",
    "laptop restart ho gaya bina save kiye, pura code udd gaya",
    "bhai swiggy se pizza mangwate hain split karenge",
    "attendance 75 percent se kam hui to admit card nahi milega",
    "aaj practical class me experiment fail ho gaya",
    "bhai pubg khelega kya raat ko?",
    "bus miss ho gayi, abhi auto dhundh raha hoon",
    "kal holiday hai kya college me?",
    "insta pe reel bheji hai dekh bohot funny hai",
    "meri tabiyat theek nahi hai aaj class miss karunga",
    "didi ki shaadi ki date fix ho gayi",

    # Hinglish non-sexual slang / banter (VERY IMPORTANT so model doesn't overfit on slang words)
    "tu pagal hai kya be?",
    "bhai tu bohot bada fenku hai",
    "chal nikal yahan se bakwaas mat kar",
    "kya yaar dimag ka dahi kar diya tune",
    "teri to watt lag gayi aaj exam me",
    "saale phone kyu nahi utha raha tha?",
    "kamine kab se wait kar raha hoon tera",
    "chup kar bilkul, mujhe padhne de",
    "bhai tu hero ban raha hai faltu me",
    "gajab beizzati hai yaar",

    # English everyday & professional
    "Can you share the meeting link for today?",
    "Please find attached the report for review",
    "Are we still on for lunch today at 1 PM?",
    "Don't forget to submit the assignment before midnight",
    "The train is on time, picking you up at the station",
    "Happy anniversary! Wishing you both a wonderful year",
    "Your package has been delivered to the front porch",
    "Can you help me debug this NullPointerException in Kotlin?",
    "Good morning team, standup will begin in 10 minutes",
    "Thanks for the help yesterday, really appreciated"
]

def generate_dataset():
    rows = []
    # Add adult samples (label = 1)
    for text in ADULT_SAMPLES:
        rows.append({"text": text.strip(), "label": 1})
    # Add normal samples (label = 0)
    for text in NORMAL_SAMPLES:
        rows.append({"text": text.strip(), "label": 0})

    DATASET_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(DATASET_PATH, mode="w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["text", "label"])
        writer.writeheader()
        writer.writerows(rows)

    print(f"Dataset generated at {DATASET_PATH}")
    print(f"Total rows: {len(rows)} | Adult (1): {len(ADULT_SAMPLES)} | Normal (0): {len(NORMAL_SAMPLES)}")

if __name__ == "__main__":
    generate_dataset()
