import crypto from "node:crypto";
const secret=process.env.ESN_SESSION_SECRET||crypto.randomBytes(32).toString("hex"),b64=v=>Buffer.from(JSON.stringify(v)).toString("base64url");
export function issueSession(member){const p=b64({sub:member.memberId,email:member.email,role:member.role||"member",ver:member.sessionVersion||1,exp:Date.now()+2592000000});return p+"."+crypto.createHmac("sha256",secret).update(p).digest("base64url");}
export function verifySession(token){const [p,s]=String(token||"").split(".");if(!p||!s)return null;const e=crypto.createHmac("sha256",secret).update(p).digest(),a=Buffer.from(s,"base64url");if(a.length!==e.length||!crypto.timingSafeEqual(a,e))return null;try{const d=JSON.parse(Buffer.from(p,"base64url"));return d.exp>Date.now()?d:null;}catch{return null;}}
