import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";

const scrypt = (password,salt)=>new Promise((resolve,reject)=>crypto.scrypt(password,salt,64,(e,key)=>e?reject(e):resolve(key)));
export class MemberStore {
 constructor(file=process.env.ESN_MEMBER_DATA_FILE||"/tmp/esn-members.json"){this.file=file;}
 async load(){try{return JSON.parse(await fs.readFile(this.file,"utf8"));}catch(e){if(e.code==="ENOENT")return {members:[]};throw e;}}
 async save(data){await fs.mkdir(path.dirname(this.file),{recursive:true});const tmp=this.file+".tmp";await fs.writeFile(tmp,JSON.stringify(data,null,2),{mode:0o600});await fs.rename(tmp,this.file);}
 async register({email,password,name}){
  const data=await this.load(); const normalized=email.trim().toLowerCase();
  if(data.members.some(x=>x.email===normalized)) throw Object.assign(new Error("An account already exists for that email"),{status:409});
  const salt=crypto.randomBytes(16).toString("hex"); const key=await scrypt(password,salt);
  const member={memberId:"esn_"+crypto.randomUUID(),email:normalized,name:name.trim(),passwordHash:key.toString("hex"),passwordSalt:salt,createdAt:new Date().toISOString()};
  data.members.push(member); await this.save(data); return member;
 }
 async authenticate({email,password}){
  const data=await this.load(); const member=data.members.find(x=>x.email===email.trim().toLowerCase()&&x.passwordHash);
  if(!member) return null; const key=await scrypt(password,member.passwordSalt);
  const a=Buffer.from(member.passwordHash,"hex"),b=Buffer.from(key);
  return a.length===b.length&&crypto.timingSafeEqual(a,b)?member:null;
 }
 async findOrCreate(identity){
  const data=await this.load();let member=data.members.find(x=>x.googleSubject===identity.googleSubject);
  if(!member){member={memberId:"esn_"+crypto.randomUUID(),googleSubject:identity.googleSubject,email:identity.email,name:identity.name,createdAt:new Date().toISOString()};data.members.push(member);await this.save(data);} return member;
 }
}