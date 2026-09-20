import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";
export class MemberStore {
 constructor(file=process.env.ESN_MEMBER_DATA_FILE||"/tmp/esn-members.json"){this.file=file;}
 async load(){try{return JSON.parse(await fs.readFile(this.file,"utf8"));}catch(e){if(e.code==="ENOENT")return {members:[]};throw e;}}
 async save(data){await fs.mkdir(path.dirname(this.file),{recursive:true});const tmp=this.file+".tmp";await fs.writeFile(tmp,JSON.stringify(data,null,2),{mode:0o600});await fs.rename(tmp,this.file);}
 async findOrCreate(identity){
  const data=await this.load();let member=data.members.find(x=>x.googleSubject===identity.googleSubject);
  if(!member){member={memberId:"esn_"+crypto.randomUUID(),googleSubject:identity.googleSubject,email:identity.email,name:identity.name,createdAt:new Date().toISOString()};data.members.push(member);await this.save(data);}
  return member;
 }
}