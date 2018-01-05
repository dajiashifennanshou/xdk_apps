package com.develop.df.transfermachine.data;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/9/19.
 */
public class Person implements Serializable{
    public String getSchoolcode() {
        return schoolcode;
    }

    public void setSchoolcode(String schoolcode) {
        this.schoolcode = schoolcode;
    }

    public String getAccountid() {
        return accountid;
    }

    public void setAccountid(String accountid) {
        this.accountid = accountid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    private String schoolcode,accountid,name,pid,department;
    private double money;

}
