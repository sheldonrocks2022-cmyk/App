import crypto from "node:crypto";
const secret=process.env.ESN_SESSION_SECRET||crypto.randomBytes(32).toString("hex");
const b64=v=>Buffer.from(JSON.stringify(v)).toString("base64url");
export function issueSession(member){const payload=b64({sub:member.memberId,email:member.email,exp:Date.now()+1000*60*60*24*30});const sig=crypto.createHmac("sha256",secret).update(payload).digest("base64url");return payload+"."+sig;}
export function verifySession(token){const [payload,sig]=String(token||"").split(".");if(!payload||!sig)return null;const expected=crypto.createHmac("sha256",secret).update(payload).digest();let actual;try{actual=Buffer.from(sig,"base64url");}catch{return null;}if(actual.length!==expected.length||!crypto.timingSafeEqual(actual,expected))return null;const data=JSON.parse(Buffer.from(payload,"base64url"));return data.exp>Date.now()?data:null;}
