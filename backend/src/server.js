import http from "node:http";
import { GoogleAuthService } from "./services/googleAuth.js";
import { MemberStore } from "./services/memberStore.js";
import { issueSession,verifySession } from "./services/sessionTokens.js";
const port=Number(process.env.PORT||8080), audience=process.env.GOOGLE_WEB_CLIENT_ID||"";
const google=new GoogleAuthService(audience), members=new MemberStore(process.env.ESN_MEMBER_DATA_FILE);
function json(res,status,body){res.writeHead(status,{"content-type":"application/json","cache-control":"no-store","x-content-type-options":"nosniff"});res.end(JSON.stringify(body));}
async function body(req){const chunks=[];let n=0;for await(const c of req){n+=c.length;if(n>16384)throw Object.assign(new Error("Request too large"),{status:413});chunks.push(c);}try{return JSON.parse(Buffer.concat(chunks).toString()||"{}");}catch{throw Object.assign(new Error("Invalid JSON"),{status:400});}}
const validEmail=x=>typeof x==="string"&&/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(x)&&x.length<=254;
const validPassword=x=>typeof x==="string"&&x.length>=10&&x.length<=128;
const server=http.createServer(async(req,res)=>{try{
 if(req.method==="GET"&&req.url==="/health")return json(res,200,{ok:true,service:"esn-hub-backend"});
 if(req.method==="POST"&&req.url==="/v1/auth/register"){const b=await body(req);if(!validEmail(b.email)||!validPassword(b.password)||typeof b.name!=="string"||b.name.trim().length<2||b.name.length>50)return json(res,400,{error:"Use a valid email, a 2-50 character name, and a password of at least 10 characters"});const m=await members.register(b);return json(res,201,{memberId:m.memberId,name:m.name,email:m.email,sessionToken:issueSession(m)});}
 if(req.method==="POST"&&req.url==="/v1/auth/login"){const b=await body(req);if(!validEmail(b.email)||typeof b.password!=="string")return json(res,400,{error:"Invalid credentials"});const m=await members.authenticate(b);if(!m)return json(res,401,{error:"Invalid credentials"});return json(res,200,{memberId:m.memberId,name:m.name,email:m.email,sessionToken:issueSession(m)});}
 if(req.method==="GET"&&req.url==="/v1/auth/session"){const token=(req.headers.authorization||"").replace(/^Bearer\s+/i,"");const s=verifySession(token);return s?json(res,200,{memberId:s.sub,email:s.email}):json(res,401,{error:"Invalid session"});}
 if(req.method==="POST"&&req.url==="/v1/auth/google"){if(!audience)return json(res,503,{error:"Google authentication is not configured"});const b=await body(req);if(typeof b.idToken!=="string")return json(res,400,{error:"idToken is required"});const identity=await google.verify(b.idToken);const m=await members.findOrCreate(identity);return json(res,200,{memberId:m.memberId,name:m.name,email:m.email,sessionToken:issueSession(m)});}
 return json(res,404,{error:"Not found"});
}catch(e){const status=Number(e.status)||500;return json(res,status,{error:status>=500?"Server error":e.message});}});
server.listen(port,"0.0.0.0",()=>console.log("ESN Hub backend listening on "+port));
