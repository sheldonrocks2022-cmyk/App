import http from "node:http";
import crypto from "node:crypto";
import { GoogleAuthService } from "./services/googleAuth.js";
import { MemberStore } from "./services/memberStore.js";

const port = Number(process.env.PORT || 8080);
const audience = process.env.GOOGLE_WEB_CLIENT_ID || "";
const auth = new GoogleAuthService(audience);
const members = new MemberStore(process.env.ESN_MEMBER_DATA_FILE);

function json(res,status,body){const data=JSON.stringify(body);res.writeHead(status,{"content-type":"application/json","cache-control":"no-store","x-content-type-options":"nosniff"});res.end(data);}
async function readJson(req){const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;if(size>16384) throw Object.assign(new Error("Request too large"),{status:413});chunks.push(chunk);}try{return JSON.parse(Buffer.concat(chunks).toString("utf8")||"{}");}catch{throw Object.assign(new Error("Invalid JSON"),{status:400});}}
const server=http.createServer(async(req,res)=>{
 try{
  if(req.method==="GET"&&req.url==="/health") return json(res,200,{ok:true,service:"esn-hub-backend"});
  if(req.method==="POST"&&req.url==="/v1/auth/google"){
   if(!audience) return json(res,503,{error:"Google authentication is not configured"});
   const body=await readJson(req); if(typeof body.idToken!=="string"||body.idToken.length<20) return json(res,400,{error:"idToken is required"});
   const identity=await auth.verify(body.idToken);
   const member=await members.findOrCreate(identity);
   return json(res,200,{memberId:member.memberId,name:member.name,email:member.email});
  }
  return json(res,404,{error:"Not found"});
 }catch(error){
  const status=Number(error.status)||401;
  return json(res,status,{error:status>=500?"Server error":error.message});
 }
});
server.listen(port,"0.0.0.0",()=>console.log("ESN Hub backend listening on "+port));
