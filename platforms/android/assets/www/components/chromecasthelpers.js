define([],function(){function A(A){var E=LinkParser.parse(A);return 1==E.length}function E(A){return A=A.toLowerCase(),-1!=A.indexOf("127.0.0.1")?!0:-1!=A.indexOf("localhost")?!0:!1}function I(I){var O=I.serverAddress();if(A(O)&&!E(O))return new Promise(function(A){A(O)});var R=S[O];return R?new Promise(function(A){A(R)}):I.getJSON(I.getUrl("System/Endpoint")).then(function(A){return A.IsInNetwork?I.getPublicSystemInfo().then(function(A){return S[O]=A.LocalAddress,A.LocalAddress}):(S[O]=O,O)})}function O(){S={}}!function(){function A(A){return A.match(M)||(A="http://"+A),A}var E="(?:(?:http|https|rtsp|ftp):\\/\\/)",I="(?:(?:[a-z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-f0-9]{2})){1,64}(?:\\:(?:[a-z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-f0-9]{2})){1,25})?\\@)",O="(?:[a-z0-9][a-z0-9\\-]{0,64}\\.)+",S="(?:[a-z]{2,})",R="(?:(?:www|ftp)\\.)",C="(?:A[CDEFGILMNOQRSTUWXZ]|ACADEMY|ACCOUNTANTS|ACTOR|AERO|AGENCY|AIRFORCE|ARCHI|ARPA|ASIA|ASSOCIATES|AUDIO|AUTOS|AXA|B[ABDEFGHIJMNORSTVWYZ]|BAR|BARGAINS|BAYERN|BEER|BERLIN|BEST|BID|BIKE|BIZ|BLACK|BLACKFRIDAY|BLUE|BOUTIQUE|BUILD|BUILDERS|BUZZ|C[ACDFGHIKLMNORUVWXYZ]|CAB|CAMERA|CAMP|CAPITAL|CARDS|CARE|CAREER|CAREERS|CASH|CAT|CATERING|CENTER|CEO|CHEAP|CHRISTMAS|CHURCH|CITIC|CLAIMS|CLEANING|CLINIC|CLOTHING|CLUB|CODES|COFFEE|COLLEGE|COLOGNE|COM|COMMUNITY|COMPANYCOMPUTER|CONDOS|CONSTRUCTION|CONSULTING|CONTRACTORS|COOKING|COOL|COOP|COUNTRY|CREDIT|CREDITCARD|CRUISES|D[EJKMOZ]|DANCE|DATING|DEMOCRAT|DENTAL|DESI|DIAMONDS|DIGITAL|DIRECTORY|DISCOUNT|DNP|DOMAINS|E[CEGRSTU]|EDU|EDUCATION|EMAIL|ENGINEERING|ENTERPRISES|EQUIPMENT|ESTATE|EUS|EVENTS|EXCHANGE|EXPERT|EXPOSED|F[IJKMOR]|FAIL|FARM|FEEDBACK|FINANCE|FINANCIAL|FISH|FISHING|FITNESS|FLIGHTS|FLORIST|FOO|FOUNDATION|FROGANS|FUND|FURNITURE|FUTBOL|G[ABDEFGHILMNPQRSTUWY]|GAL|GALLERY|GIFT|GLASS|GLOBO|GMO|GOP|GOV|GRAPHICS|GRATIS|GRIPE|GUIDE|GUITARS|GURU|H[KMNRTU]|HAUS|HIPHOP|HOLDINGS|HOLIDAY|HOMES|HORSE|HOUSE|I[DELMNOQRST]|IMMOBILIEN|INDUSTRIES|INFO|INK|INSTITUTE|INSURE|INT|INTERNATIONAL|INVESTMENTS|J[EMOP]|JETZT|JOBS|JUEGOS|K[EGHIMNPRWYZ]|KAUFEN|KIM|KITCHEN|KIWI|KOELN|KRED|L[ABCIKRSTUVY]|LAND|LEASE|LIFE|LIGHTING|LIMITED|LIMO|LINK|LOANS|LONDON|LUXE|LUXURY|M[ACDEGHKLMNOPQRSTUVWXYZ]|MAISON|MANAGEMENT|MANGO|MARKETING|MEDIA|MEET|MENU|MIAMI|MIL|MOBI|MODA|MOE|MONASH|MOSCOW|MOTORCYCLES|MUSEUM|N[ACEFGILOPRUZ]|NAGOYA|NAME|NET|NEUSTAR|NINJA|NYC|O[M]|OKINAWA|ONL|ORG|P[AEFGHKLMNRSTWY]|PARIS|PARTNERS|PARTS|PHOTO|PHOTOGRAPHY|PHOTOS|PICS|PICTURES|PINK|PLUMBING|POST|PRO|PRODUCTIONS|PROPERTIES|PUB|Q[A]|QPON|QUEBEC|R[EOSUW]|RECIPES|RED|REISE|REISEN|REN|RENTALS|REPAIR|REPORT|REST|REVIEWS|RICH|RIO|ROCKS|RODEO|RUHR|RYUKYU|S[ABCDEGHIJKLMNORTUVXYZ]|SAARLAND|SCHULE|SERVICES|SEXY|SHIKSHA|SHOES|SINGLES|SOCIAL|SOHU|SOLAR|SOLUTIONS|SOY|SUPPLIES|SUPPLY|SUPPORT|SURGERY|SYSTEMS|T[CDFGHJKLMNOPRTVWZ]|TATTOO|TAX|TECHNOLOGY|TEL|TIENDA|TIPS|TODAY|TOKYO|TOOLS|TOWN|TOYS|TRADE|TRAINING|TRAVEL|U[AGKSYZ]|UNIVERSITY|UNO|V[ACEGINU]|VACATIONS|VEGAS|VENTURES|VERSICHERUNG|VIAJES|VILLAS|VISION|VODKA|VOTE|VOTING|VOTO|VOYAGE|W[FS]|WANG|WATCH|WEBCAM|WED|WIEN|WIKI|WORKS|WTC|WTF|XXX|XYZ|Y[ET]|YACHTS|YOKOHAMA|Z[AMW]|ZONE|XN--(?:3BST00M|3DS443G|3E0B707E|45BRJ9C|55QW42G|55QX5D|6FRZ82G|6QQ986B3XL|80ADXHKS|80AO21A|80ASEHDB|80ASWG|90A3AC|C1AVG|CG4BKI|CLCHC0EA0B2G2A9GCD|CZR694B|CZRU2D|D1ACJ3B|FIQ228C5HS|FIQ64B|FIQS8S|FIQZ9S|FPCRJ9C3D|FZC2C9E2C|GECRJ9C|H2BRJ9C|I1B6B1A6A2E|IO0A7I|J1AMH|J6W193G|KPRW13D|KPRY57D|L1ACC|LGBBAT1AD8J|MGB9AWBF|MGBA3A4F16A|MGBAAM7A8H|MGBAB2BD|MGBAYH7GPA|MGBBH1A71E|MGBC0A9AZCG|MGBERP4A5D4AR|MGBX4CD0AB|NGBC5AZD|NQV7F|NQV7FS00EMA|O3CW4H|OGBPF8FL|P1AI|PGBS0DH|Q9JYB4C|RHQV96G|S9BRJ9C|SES554G|UNUP4Y|WGBH1C|WGBL6A|XKC2AL3HYE2A|XKC2DL3A5EE0H|YFRO4I67O|YGBI2AMMX|ZFR164B)|الاردن|中國|中国|امارات|香港|ایران|ලංකා|இலங்கை|مصر|قطر|РФ|سورية|台灣|台湾)",N="((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?",T="(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])",G="(?:"+E+I+"?"+O+S+"|(?!"+E+")"+I+"?(?:"+O+C+"|"+R+O+S+"|"+O+S+"(?=\\/))|"+E+"?"+I+"?(?:"+N+"|"+T+"))(?:\\:\\d{1,5})?(?:\\/(?:(?:[a-z0-9\\/\\@\\&\\#\\~\\*\\_\\-\\+])|(?:\\%[a-f0-9]{2})|(?:[\\;\\?\\:\\.\\!\\'\\(\\)\\,\\=]+(?=(?:[a-z0-9\\/\\@\\&\\#\\~\\*\\_\\-\\+])|(?:\\%[a-f0-9]{2}))))*|\\b|$)",L=RegExp(G,"gi"),M=RegExp("^"+E,"i"),U={parse:function(E){for(var I=[];match=L.exec(E);){var O=match[0],S=match.index,R=O.length,C=A(E);I.push({pos:S,text:O,len:R,url:C})}return I}};window.LinkParser=U}();var S={};return Events.on(ConnectionManager,"localusersignedin",O),Events.on(ConnectionManager,"localusersignedout",O),setInterval(O,18e4),{getServerAddress:I}});