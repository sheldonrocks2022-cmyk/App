import http from "node:http";
import {MemberStore} from "./services/memberStore.js";
import {issueSession,verifySession} from "./services/sessionTokens.js";

const port=Number(process.env.PORT||8080);
const adminEmail=(process.env.ESN_ADMIN_EMAIL||"").trim().toLowerCase();
const members=new MemberStore(process.env.ESN_MEMBER_DATA_FILE);
const attempts=new Map();

function json(r,s,b){r.writeHead(s,{"content-type":"application/json","cache-control":"no-store","x-content-type-options":"nosniff"});r.end(JSON.stringify(b));}
async function body(q){const c=[];let n=0;for await(const x of q){n+=x.length;if(n>16384)throw Object.assign(new Error("Request too large"),{status:413});c.push(x);}try{return JSON.parse(Buffer.concat(c).toString()||"{}");}catch{throw Object.assign(new Error("Invalid JSON"),{status:400});}}
const email=x=>typeof x==="string"&&/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(x)&&x.length<=254;
const pw=x=>typeof x==="string"&&x.length>=10&&x.length<=128;
function limited(q,bucket,max=30,windowMs=900000){const key=(q.socket.remoteAddress||"unknown")+":"+bucket,now=Date.now();let e=attempts.get(key);if(!e||e.reset<now)e={count:0,reset:now+windowMs};e.count++;attempts.set(key,e);return e.count>max;}
async function authenticated(q){const s=verifySession((q.headers.authorization||"").replace(/^Bearer\s+/i,""));if(!s)return null;const m=await members.getById(s.sub);return m&&(m.sessionVersion||1)===s.ver?m:null;}
async function loginMember(b){const m=email(b.email)&&typeof b.password==="string"?await members.authenticate(b):null;if(!m)return null;if(m.email===adminEmail)await members.ensureAdmin(m.email);return members.getById(m.memberId);}
function safe(m){return{memberId:m.memberId,name:m.name,email:m.email,role:m.role||"member",credits:m.credits||0,createdAt:m.createdAt};}\nfunction authPayload(m){return{...safe(m),sessionToken:issueSession(m)};}

const server=http.createServer(async(q,r)=>{try{
    if(q.method==="GET"&&q.url==="/health")return json(r,200,{ok:true,service:"esn-hub-backend"});
    if(q.method==="POST"&&q.url==="/v1/auth/register"){
        if(limited(q,"register",10))return json(r,429,{error:"Too many account attempts. Try again later."});
        const b=await body(q);
        if(!email(b.email)||!pw(b.password)||typeof b.name!=="string"||b.name.trim().length<2||b.name.length>50||typeof b.securityQuestion!=="string"||b.securityQuestion.trim().length<8||b.securityQuestion.length>120||typeof b.securityAnswer!=="string"||b.securityAnswer.trim().length<3||b.securityAnswer.length>120)return json(r,400,{error:"Use a valid email, a 2-50 character name, a 10+ character password, and a security question and answer."});
        const m=await members.register(b);if(m.email===adminEmail)await members.ensureAdmin(m.email);return json(r,201,authPayload(await members.getById(m.memberId)));
    }
    if(q.method==="POST"&&q.url==="/v1/auth/login"){
        if(limited(q,"login"))return json(r,429,{error:"Too many login attempts. Try again later."});
        const m=await loginMember(await body(q));return m?json(r,200,authPayload(m)):json(r,401,{error:"Invalid credentials"});
    }
    if(q.method==="POST"&&q.url==="/v1/auth/admin-login"){
        if(limited(q,"admin",12))return json(r,429,{error:"Too many admin login attempts. Try again later."});
        const m=await loginMember(await body(q));if(!m)return json(r,401,{error:"Invalid credentials"});if(m.role!=="admin")return json(r,403,{error:"This ESN account is not authorized for admin access"});return json(r,200,authPayload(m));
    }
    if(q.method==="POST"&&q.url==="/v1/auth/recovery-question"){
        if(limited(q,"recovery",20))return json(r,429,{error:"Too many recovery attempts. Try again later."});
        const b=await body(q),x=email(b.email)?await members.getSecurityQuestion(b.email):null;return x?json(r,200,x):json(r,404,{error:"Account recovery is unavailable"});
    }
    if(q.method==="POST"&&q.url==="/v1/auth/reset-password"){
        if(limited(q,"reset",12))return json(r,429,{error:"Too many reset attempts. Try again later."});
        const b=await body(q);if(!email(b.email)||!pw(b.newPassword)||typeof b.securityAnswer!=="string")return json(r,400,{error:"Invalid reset request"});
        const m=await members.resetWithSecurityAnswer(b);return m?json(r,200,{message:"Password reset successful"}):json(r,401,{error:"Recovery answer did not match"});
    }
    if(q.method==="GET"&&q.url==="/v1/auth/session"){const m=await authenticated(q);return m?json(r,200,safe(m)):json(r,401,{error:"Invalid session"});}\n    if(q.method==="PATCH"&&q.url==="/v1/account/profile"){const m=await authenticated(q);if(!m)return json(r,401,{error:"Invalid session"});const updated=await members.updateProfile(m.memberId,await body(q));return json(r,200,safe(updated));}
    if(q.method==="GET"&&q.url==="/v1/admin/members"){const m=await authenticated(q);if(!m||m.role!=="admin")return json(r,403,{error:"Admin access required"});return json(r,200,{members:await members.listSafe()});}\n    if(q.method==="PATCH"&&q.url?.startsWith("/v1/admin/members/")){const admin=await authenticated(q);if(!admin||admin.role!=="admin")return json(r,403,{error:"Admin access required"});const id=decodeURIComponent(q.url.split("/")[4]||""),b=await body(q);if(id===admin.memberId&&b.role&&b.role!=="admin")return json(r,400,{error:"Owners cannot remove their own admin access"});let m=await members.getById(id);if(!m)return json(r,404,{error:"Member not found"});if(b.role!==undefined)m=await members.setRole(id,b.role);if(b.credits!==undefined)m=await members.setCredits(id,b.credits);return json(r,200,safe(await members.getById(id)));}
    return json(r,404,{error:"Not found"});
}catch(e){const s=Number(e.status)||500;return json(r,s,{error:s>=500?"Server error":e.message});}});
server.listen(port,"0.0.0.0",()=>console.log("ESN Hub backend listening on "+port));
